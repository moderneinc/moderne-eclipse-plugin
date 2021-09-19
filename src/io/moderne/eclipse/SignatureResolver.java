package io.moderne.eclipse;

import java.util.StringJoiner;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

// Modified from https://github.com/eclipse/eclemma/blob/master/org.eclipse.eclemma.core/src/org/eclipse/eclemma/internal/core/analysis/SignatureResolver.java
// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=502563

public class SignatureResolver {
	private SignatureResolver() {
	}
	
	public static String getParameters(String signature) {
		int pos = signature.lastIndexOf(')');
		// avoid String instances for methods without parameters
		return pos == 1 ? "" : signature.substring(1, pos);
	}

	public static String getParameters(IMethod method) throws JavaModelException {
		if (method.isBinary()) {
			return getParameters(method.getSignature());
		}
		StringJoiner types = new StringJoiner(",");
		String[] parameterTypes = method.getParameterTypes();
		for (String t : parameterTypes) {
			StringBuilder type = new StringBuilder();
			int arrayCount = Signature.getArrayCount(t);
			type.append(resolveParameterType(method, t.substring(arrayCount)));
			for (int i = 0; i < arrayCount; i++) {
				type.append("[]");
			}
			types.add(type.toString());
		}
		return types.toString();
	}

	private static String resolveParameterType(IMethod method, String parameterType)
			throws JavaModelException {
		char kind = parameterType.charAt(0);
		switch (kind) {
		case Signature.C_UNRESOLVED:
			String identifier = parameterType.substring(1, parameterType.length() - 1);
			String type;
			if ((type = resolveType(method.getDeclaringType(), identifier)) != null) {
				return type;
			}
			if ((type = resolveTypeParameter(method, identifier)) != null) {
				return type;
			}
			break;
		}
		return parameterType;
	}

	private static String resolveType(IType scope, String identifier) throws JavaModelException {
		String[][] types = scope.resolveType(Signature.getTypeErasure(identifier));
		if (types == null || types.length != 1) {
			return null;
		}
		
		StringBuilder type = new StringBuilder();
		String qualifier = types[0][0];
		if (qualifier.length() > 0) {
			type.append(qualifier).append('.');
		}
		type.append(types[0][1]);
		return type.toString();
	}

	private static String resolveTypeParameter(IMethod method, String identifier) throws JavaModelException {
		String typeParam;
		IType type = method.getDeclaringType();
		if ((typeParam = resolveTypeParameter(type, method.getTypeParameters(), identifier)) != null) {
			return typeParam;
		}
		while (type != null) {
			if ((typeParam = resolveTypeParameter(type, type.getTypeParameters(), identifier)) != null) {
				return typeParam;
			}
			type = type.getDeclaringType();
		}
		return null;
	}

	private static String resolveTypeParameter(IType context, ITypeParameter[] typeParameters, String identifier) throws JavaModelException {
		for (ITypeParameter p : typeParameters) {
			if (identifier.equals(p.getElementName())) {
				String[] bounds = p.getBounds();
				if (bounds.length == 0) {
					return "java.lang.Object";
				} else {
					return resolveType(context, bounds[0]);
				}
			}
		}
		return null;
	}
}