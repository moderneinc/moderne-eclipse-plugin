package io.moderne.eclipse;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class ModerneHandler extends AbstractHandler {
	private static final String FIND_TYPES = "mutation {runRecipe(run: {recipe:{id:\"org.openrewrite.java.search.FindTypes\",options:[{name:\"fullyQualifiedTypeName\",value:\"%s\"}]}}) {id}}";
	private static final String FIND_METHODS = "mutation {runRecipe(run: {recipe:{id:\"org.openrewrite.java.search.FindMethods\",options:[{name:\"methodPattern\",value:\"%s\"}]}}) {id}}";
	private static final String FIND_FIELDS = "mutation {runRecipe(run: {recipe:{id:\"org.openrewrite.java.search.FindFields\",options:[{name:\"fullyQualifiedTypeName\",value:\"%s\"},{name:\"fieldName\",value:\"%s\"}]}}) {id}}";

	private static final URL url;

	static {
		try {
			url = new URL("https://api.moderne.io/graphql");
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e);
		}
	}

	private final Path tokenFile = new File(System.getProperty("user.home") + "/.moderne/token.txt").toPath();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(!tokenFile.toFile().exists()) {
			try {
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
					.openURL(new URL("https://app.moderne.io/welcome/plugins/IntelliJ"));
			} catch (PartInitException | MalformedURLException e) {
				throw new RuntimeException(e);
			}		
			return null;
		}
		
		Object target = null;
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		if (selection instanceof ITextSelection) {
			// get from active editor
			IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.getActiveEditor();
			if (activeEditor instanceof JavaEditor) {
				try {
					IJavaElement[] javas = SelectionConverter.codeResolve(((JavaEditor) activeEditor));
					target = javas[0];
				} catch (JavaModelException e) {
					throw new IllegalStateException(e);
				}
			} else {
				throw new IllegalStateException("Unable to get a target Java element");
			}
		} else if (selection instanceof ITreeSelection) {
			// package explorer
			target = ((ITreeSelection) selection).getFirstElement();
		}

		String request = null;
		if (target instanceof IType) {
			request = String.format(FIND_TYPES, ((IType) target).getFullyQualifiedName());
		} else if (target instanceof IMethod) {
			IMethod method = (IMethod) target;
			try {
				String params = SignatureResolver.getParameters(method);
				params = Arrays.stream(params.split(";")).map(p -> {
					while (p.startsWith("[")) {
						p = p.substring(1) + "[]";
					}
					return p;
				}).map(p -> p.replaceAll("^L", "")).map(p -> p.replace("/", ".")).map(p -> p.replace("$", "."))
						.collect(Collectors.joining(","));

				String name = method.isConstructor() ? "<constructor>" : method.getElementName();
				String methodPattern = method.getDeclaringType().getFullyQualifiedName() + " " + name + "(" + params
						+ ")";
				request = String.format(FIND_METHODS, methodPattern);
			} catch (JavaModelException e) {
				return null;
			}
		} else if (target instanceof IField) {
			IField field = (IField) target;
			request = String.format(FIND_FIELDS, field.getDeclaringType().getFullyQualifiedName(),
					field.getElementName());
		}

		String requestEscaped = "{\"query\":\"" + request.replace("\"", "\\\"")
				+ "\",\"variables\":{},\"operationName\":null}";

		try {
			byte[] token = Files.readAllBytes(tokenFile);

			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			
			SSLContext sc = SSLContext.getInstance("TLSv1.2");
			sc.init(null, null, null);
			
			connection.setSSLSocketFactory(sc.getSocketFactory());
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + new String(token).trim());
			connection.setDoOutput(true);

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = requestEscaped.getBytes("utf-8");
				os.write(input, 0, input.length);
			}

			try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
				StringBuilder response = new StringBuilder();
				String responseLine = null;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
				
				String responseStr = response.toString();
				if (responseStr.contains("id")) {
					Matcher matcher = Pattern.compile("(\\w+)\\\"\\}").matcher(responseStr);
					if (matcher.find()) {
						String runId = matcher.group(1);
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
								.openURL(new URL("https://app.moderne.io/results/" + runId));
					}
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		return null;
	}
}
