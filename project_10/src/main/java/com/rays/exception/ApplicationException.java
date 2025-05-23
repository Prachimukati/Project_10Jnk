package com.rays.exception;

/**
 * ApplicationException is propogated from Service classes when an business
 * logic exception occurered.
 *Dipanshi Mukati 
 */
public class ApplicationException extends Exception {

	/**
	 * @param msg
	 *            : Error message
	 */
	public ApplicationException(String msg) {      //checked exception
		super(msg);
	}
}