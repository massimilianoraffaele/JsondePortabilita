package com.jsonde.gui.sdedit;

import com.jsonde.client.dao.DaoException;
import com.jsonde.client.dao.DaoFactory;
import com.jsonde.client.domain.Method;
import com.jsonde.client.domain.MethodCall;
import com.jsonde.gui.Main;
import com.jsonde.util.file.FileUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/**
 * 
 * @author admin
 *
 */
public class SdEditDataRenderer {

    private final Set<Integer> addedClassNames = new HashSet<Integer>();
    private final Map<String, Integer> classLevelMap = new HashMap<String, Integer>();

    private final Map<String, String> shortClassNamesMap = new HashMap<String, String>();
    private final Set<String> classesWithAmbigousNames = new HashSet<String>();

    /**
     * 
     * @param methodCall
     * @param method
     * @return
     * @throws DaoException
     */
    private String getClassName(MethodCall methodCall, Method method) throws DaoException {

        String fullClassName = null;
        String shortClassName = null;
		try {
			fullClassName = null == methodCall.getActualClassId() ?
			        DaoFactory.getClazzDao().get(method.getClassId()).getName() :
			        DaoFactory.getClazzDao().get(methodCall.getActualClassId()).getName();
		

		shortClassName = fullClassName.substring(
                fullClassName.lastIndexOf('.') + 1
        );} 
        catch (SQLException e) {
		}

        if (classesWithAmbigousNames.contains(shortClassName)) {
            return fullClassName;
        } else {
            return shortClassName;
        }

    }

    private void fillShortClassNamesMap(MethodCall methodCall) throws DaoException {

        Method method = null;
		try {
			method = DaoFactory.getMethodDao().get(
			        methodCall.getMethodId()
			);
		} catch (SQLException e1) {
		}

        String fullClassName = null;
        String shortClassName = null;
		try {
			fullClassName = null == methodCall.getActualClassId() ?
			        DaoFactory.getClazzDao().get(method.getClassId()).getName() :
			        DaoFactory.getClazzDao().get(methodCall.getActualClassId()).getName();


			    shortClassName = fullClassName.substring(
                fullClassName.lastIndexOf('.') + 1
        );
		} catch (SQLException e1) {
			System.out.println("Something was wrong");
		}

        if (shortClassNamesMap.containsKey(shortClassName)) {

            String existingFullClassName = shortClassNamesMap.get(shortClassName);

            if (!fullClassName.equals(existingFullClassName)) {
                classesWithAmbigousNames.add(shortClassName);
            }

        } else {
            shortClassNamesMap.put(shortClassName, fullClassName);
        }

        try {
			for (MethodCall callee :
			        DaoFactory.getMethodCallDao().getByCondition("CALLERID = ?", methodCall.getId())) {
			    fillShortClassNamesMap(callee);
			}
		} catch (SQLException e) {

		}

    }

    /**
     * 
     * @param methodCall
     * @return
     * @throws GeneralException
     */
    public String processMethodCall(MethodCall methodCall) throws GeneralException{

        try {

            fillShortClassNamesMap(methodCall);

            Method method = null;
			try {
				method = DaoFactory.getMethodDao().get(
				        methodCall.getMethodId()
				);
			} catch (SQLException e) {
			}

            String methodName = method.getName();

            String className = getClassName(methodCall, method);

            Writer diagramWriter = new StringWriter();

            diagramWriter.append("user:Actor").append(FileUtils.LINE_SEPARATOR);

            writeObjectNames(methodCall, diagramWriter);

            diagramWriter.append(FileUtils.LINE_SEPARATOR);

            diagramWriter.
                    append("user:").
                    append(className.replaceAll("\\.", "\\\\.")).
                    append(".").
                    append(methodName).
                    append(FileUtils.LINE_SEPARATOR);

            writeMethodNames(methodCall, diagramWriter);

            diagramWriter.flush();

            return diagramWriter.toString();

        } catch (IOException e) {
            Main.getInstance().processException(e);
            throw new GeneralException();
        } catch (DaoException e) {
            Main.getInstance().processException(e);
            throw new GeneralException();
        }

    }

    protected void writeObjectNames(MethodCall methodCall, Writer diagramWriter) throws IOException, DaoException {

        Method method = null;
		try {
			method = DaoFactory.getMethodDao().get(
			        methodCall.getMethodId()
			);
		} catch (SQLException e1) {
		}

        String className = getClassName(methodCall, method);

        int classNameHash = className.hashCode();

        if (!addedClassNames.contains(classNameHash)) {
            addedClassNames.add(classNameHash);
            diagramWriter.
                    append(className).
                    append(':').
                    append(className).
                    append(FileUtils.LINE_SEPARATOR);
        }

        try {
			for (MethodCall callee :
			        DaoFactory.getMethodCallDao().getByCondition("CALLERID = ?", methodCall.getId())) {
			    writeObjectNames(callee, diagramWriter);
			}
		} catch (SQLException e) {
		}

    }

    protected void writeMethodNames(MethodCall methodCall, Writer diagramWriter) throws IOException, DaoException {

        Method method = null;
		try {
			method = DaoFactory.getMethodDao().get(
			        methodCall.getMethodId()
			);
		} catch (SQLException e1) {
		}

        String callerClassName = getClassName(methodCall, method);

        classLevelMap.put(callerClassName, 0);

        try {
			for (MethodCall callee :
			        DaoFactory.getMethodCallDao().getByCondition("CALLERID = ?", methodCall.getId())) {

			    Method calleeMethod = DaoFactory.getMethodDao().get(
			            callee.getMethodId()
			    );

			    String calleeClassName = getClassName(callee, calleeMethod);

			    int level = classLevelMap.get(callerClassName);

			    diagramWriter.
			            append(callerClassName).
			            append('[').
			            append(Integer.toString(level)).
			            append(']').
			            append(':').
			            append(calleeClassName.replaceAll("\\.", "\\\\.")).
			            append('.').
			            append(calleeMethod.getName()).
			            append(FileUtils.LINE_SEPARATOR);

			    classLevelMap.put(callerClassName, 0);

			    writeMethodNames(callee, diagramWriter);

			    if (callerClassName.equals(calleeClassName)) {
			        classLevelMap.put(callerClassName, 1 + classLevelMap.get(callerClassName));
			    }

			}
		} catch (SQLException e) {
		}

    }

}
