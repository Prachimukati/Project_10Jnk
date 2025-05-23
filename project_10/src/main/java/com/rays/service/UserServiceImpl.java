package com.rays.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rays.common.BaseServiceImpl;
import com.rays.common.UserContext;
import com.rays.common.mail.EmailDTO;
import com.rays.common.mail.EmailServiceImpl;
import com.rays.dao.UserDAOInt;
import com.rays.dto.UserDTO;

@Service
@Transactional
public class UserServiceImpl extends BaseServiceImpl<UserDTO, UserDAOInt> implements UserServiceInt {

	private static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	@Autowired
	EmailServiceImpl emailService;

	@Autowired
	UserDAOInt userDAO;

	@Override
	@Transactional(readOnly = true)
	public UserDTO findByLoginId(String login, UserContext userContext) {
		return baseDao.findByUniqueKey("loginId", login, userContext);
	}

	@Override
	public UserDTO authenticate(String loginId, String password) {
		UserDTO dto = findByLoginId(loginId, null);
		if (dto != null) {
			UserContext userContext = new UserContext(dto);
			if (password.equals(dto.getPassword())) {
				dto.setLastLogin(new Timestamp(new Date().getTime()));
				dto.setUnsucessfullLoginAttempt(0);
				update(dto, userContext);
				return dto;
			} else {
				dto.setUnsucessfullLoginAttempt(1 + dto.getUnsucessfullLoginAttempt());
				update(dto, userContext);
			}
		}
		return null;
	}

	@Override
	public UserDTO changePassword(String loginId, String oldPassword, String newPassword, UserContext userContext) {
		UserDTO dto = findByLoginId(loginId, null);
		if (oldPassword.equals(dto.getPassword())) {
			dto.setPassword(newPassword);
			update(dto, userContext);

			EmailDTO emailDTO = new EmailDTO();
			emailDTO.addTo(dto.getLoginId());
			HashMap<String, String> params = new HashMap<>();
			params.put("user", dto.getFirstName() + " " + dto.getLastName());
			emailDTO.setMessageCode("U-CP", params);
			emailService.send(emailDTO, null);

			return dto;
		}
		return null;
	}

	@Override
	public UserDTO forgotPassword(String loginId) {
		UserDTO dto = findByLoginId(loginId, null);

		UserContext userContext = new UserContext();
		userContext.setLoginId("super@nenosystems.com");
		userContext.setOrgId(0L);
		userContext.setOrgName("root");

		if (dto == null) {
			return null;
		}

		EmailDTO emailDTO = new EmailDTO();
		emailDTO.addTo(dto.getLoginId());
		HashMap<String, String> params = new HashMap<>();
		params.put("user", dto.getFirstName() + " " + dto.getLastName());
		params.put("password", dto.getPassword());
		emailDTO.setMessageCode("U-FP", params);

		emailService.send(emailDTO, null);

		return dto;
	}

	@Override
	public UserDTO register(UserDTO dto) {
		UserContext userContext = new UserContext();
		userContext.setLoginId("super@nenosystems.com");
		userContext.setOrgId(0L);
		userContext.setOrgName("root");

		Long id = add(dto, userContext);
		dto.setId(id);

		EmailDTO emailDTO = new EmailDTO();
		emailDTO.addTo(dto.getLoginId());
		HashMap<String, String> params = new HashMap<>();
		params.put("user", dto.getFirstName() + " " + dto.getLastName());
		params.put("login", dto.getLoginId());
		params.put("password", dto.getPassword());
		emailDTO.setMessageCode("U-REG", params);

		emailService.send(emailDTO, userContext);

		return dto;
	}

	@Override
	public UserDTO findByEmail(String email, UserContext userContext) {
		return userDAO.findByEmail("email", email, userContext);
	}

	// 🔐 OTP Generation and Sending Logic
	public String sendOtpToUser(String loginId) {
		UserDTO dto = findByLoginId(loginId, null);
		if (dto == null) {
			return "User not found";
		}

		// Generate 6-digit OTP
		String otp = String.format("%06d", new Random().nextInt(999999));

		// Email setup
		EmailDTO emailDTO = new EmailDTO();
		emailDTO.addTo(dto.getLoginId());
		HashMap<String, String> params = new HashMap<>();
		params.put("user", dto.getFirstName() + " " + dto.getLastName());
		params.put("otp", otp);
		emailDTO.setMessageCode("U-OTP", params);

		emailService.send(emailDTO, null);

		// (Optional) Store OTP in DTO or DB here if needed

		return otp;
	}
}