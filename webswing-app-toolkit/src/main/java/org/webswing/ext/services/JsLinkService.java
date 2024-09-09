package org.webswing.ext.services;

import java.lang.reflect.Method;

import org.webswing.model.jslink.JavaEvalRequestMsgIn;
import org.webswing.model.jslink.JsParamMsg;
import org.webswing.model.jslink.JsResultMsg;

import jdk.jsobject.JSException;

public interface JsLinkService {

	public JsParamMsg generateParam(Object arg) throws Exception;

	public JsResultMsg generateJavaResult(JavaEvalRequestMsgIn javaReq, Object result) throws Exception;

	public JsResultMsg generateJavaErrorResult(JavaEvalRequestMsgIn javaReq, Throwable result);

	public Object parseValue(JsParamMsg value) throws JSException;

	public Object[] getCompatibleParams(JavaEvalRequestMsgIn javaReq, Method m) throws Exception;

}
