package com.rays.form;

import java.util.Random;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import com.rays.common.BaseForm;

/**
 * Contains Forget Password form elements and their declarative input
 * validations.
 *Dipanshi Mukati 
 */
public class ForgetPasswordForm extends BaseForm {

	@NotEmpty
	@Email
	private String login;

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}
	/*
	 * 
	 * private Integer otp; // Add OTP field in UserDTO
	 * 
	 * public Integer getOtp() { return otp; }
	 * 
	 * public void setOtp(Integer otp) { this.otp = otp; } public Object getOtp1() {
	 * Random random = new Random(); int otp = 100000 + random.nextInt(900000);
	 * return otp; }
	 */
	

}
