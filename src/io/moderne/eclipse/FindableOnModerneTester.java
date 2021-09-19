package io.moderne.eclipse;

import java.io.File;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public class FindableOnModerneTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if(!new File(System.getProperty("user.home") + "/.moderne/token.txt").exists()) {
			return false;
		}
		
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (activeEditor instanceof JavaEditor) {
			try {
				IJavaElement[] javas = SelectionConverter.codeResolve(((JavaEditor) activeEditor));
				for (IJavaElement java : javas) {
					if(java instanceof IType || java instanceof IMethod || java instanceof IField) {
						return true;
					}
				}
				return false;
			} catch (JavaModelException e) {
				return false;
			}
		}
		return false;
	}
}
