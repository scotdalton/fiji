import common.RefreshScripts;

import ij.IJ;
import ij.ImagePlus;
import ij.Menus;

import ij.io.PluginClassLoader;

import java.io.File;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This plugin looks for Java sources in plugins/ and turns them into
 * transparently-compiling plugins.
 *
 * That means that whenever the .java file is newer than the .class file,
 * it is compiled before it is called.
 */
public class Refresh_Javas extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".java", "Java");
		setVerbose(false);
		super.run(arg);
	}

	/** Compile and run an ImageJ plugin */
	public void runScript(String path) {
		String c = path;
		if (c.endsWith(".java")) {
			c = c.substring(0, c.length() - 5);
			// TODO: show output in case of error, in a window
			try {
				if (!upToDate(path, c + ".class") &&
						!compile(path)) {
					IJ.error("Could not compile " + path);
					return;
				}
			} catch(Exception e) {
				IJ.error("Error: " + e);
				return;
			}
		}
		String pluginsPath = Menus.getPlugInsPath();
		if (!pluginsPath.endsWith(File.separator))
			pluginsPath += File.separator;
		if (c.startsWith(pluginsPath)) {
			c = c.substring(pluginsPath.length() + 1);
			while (c.startsWith(File.separator))
				c = c.substring(1);
		}
		runPlugin(c);
	}

	boolean upToDate(String source, String target) {
		File sourceFile = new File(source);
		File targetFile = new File(target);
		if (!targetFile.exists())
			return false;
		if (!sourceFile.exists())
			return true;
		return sourceFile.lastModified() < targetFile.lastModified();
	}

	static Method javac;

	boolean compile(String path) throws ClassNotFoundException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {
		String[] arguments = { path };
		if (javac == null) {
			String className = "com.sun.tools.javac.Main";
			ClassLoader loader = getClass().getClassLoader();
			Class main =
				(loader instanceof PluginClassLoader) ?
				((PluginClassLoader)loader)
					.loadClass(className, true, true) :
				loader.loadClass(className);
			Class[] argsType = new Class[] {
				arguments.getClass()
			};
			javac = main.getMethod("compile", argsType);
		}
		Object result = javac.invoke(null,
			new Object[] { arguments });
		return result.equals(new Integer(0));
	}

	void runPlugin(String className) {
		new PlugInExecuter(className);
	}
}