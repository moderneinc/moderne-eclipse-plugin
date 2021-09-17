package io.moderne.eclipse;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;

public class ModerneHandler extends AbstractHandler {

	// https://books.google.com/books?id=Uo4SlCvSy40C&pg=PA41&lpg=PA41&dq=eclipse+plugin+add+to+refactor+popup&source=bl&ots=No6LFtfSaa&sig=ACfU3U35a7bnIMske3roPhClaK2hYiF6-Q&hl=en&sa=X&ved=2ahUKEwinnOSqj4XzAhVlNn0KHfY6DfkQ6AF6BAgUEAM#v=onepage&q=eclipse%20plugin%20add%20to%20refactor%20popup&f=false
	
	// https://www.eclipse.org/forums/index.php/t/94198/
	
	// org.eclipse.jdt.core.IMember
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
//		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//		MessageDialog.openInformation(
//				window.getShell(),
//				"Moderne",
//				"Hello");
		return null;
	}
}
