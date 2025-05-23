package com.rays.ctl;

import java.util.Enumeration;
import java.util.LinkedHashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rays.common.BaseCtl;
import com.rays.common.MenuItem;
import com.rays.common.ORSResponse;
import com.rays.common.UserContext;
import com.rays.common.attachment.AttachmentDTO;
import com.rays.common.attachment.AttachmentServiceInt;
import com.rays.config.JWTUtil;
import com.rays.dto.UserDTO;
import com.rays.form.LoginForm;
import com.rays.form.UserForm;
import com.rays.form.UserRegistrationForm;
import com.rays.service.UserServiceInt;

/**
 * Login controller provides API for Sign Up, Sign In, Forgot password,
 * uploading profile pic, and menu generation.
 * 
 * Dipanshi Mukati
 */
@RestController
@RequestMapping(value = "Auth")
public class LoginCtl extends BaseCtl<UserForm, UserDTO, UserServiceInt> {

	@Autowired
	private JWTUtil jwtUtil;

	@Autowired
	private UserDetailsService jwtService; // Currently not used directly

	@Autowired
	private AttachmentServiceInt attachmentService;

	/**
	 * Find user by login id
	 */
	@GetMapping("login/{loginId}")
	public ORSResponse get(@PathVariable String loginId) {
		ORSResponse res = new ORSResponse(true);
		UserDTO dto = baseService.findByLoginId(loginId, userContext);

		if (dto != null) {
			UserDTO userDTO = new UserDTO();
			userDTO.setFirstName(dto.getFirstName());
			userDTO.setLastName(dto.getLastName());
			userDTO.setLoginId(dto.getLoginId());
			res.addData(userDTO);
		} else {
			res.setSuccess(false);
			res.addMessage("Record not found");
		}
		return res;
	}

	/**
	 * User login (authentication)
	 */
	@PostMapping("login")
	public ORSResponse login(@RequestBody @Valid LoginForm form, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) throws Exception {

		ORSResponse res = validate(bindingResult);
		if (!res.isSuccess()) {
			return res;
		}

		UserDTO dto = baseService.authenticate(form.getLoginId(), form.getPassword());

		if (dto == null) {
			res.setSuccess(false);
			res.addMessage("Invalid ID or Password");
		} else {
			UserContext context = new UserContext(dto);

			session.setAttribute("test", dto.getFirstName());

			// Add user details to response
			res.setSuccess(true);
			res.addData(dto);
			res.addResult("jsessionid", session.getId());
			res.addResult("loginId", dto.getLoginId());
			res.addResult("role", dto.getRoleName());
			res.addResult("fname", dto.getFirstName());
			res.addResult("lname", dto.getLastName());

			// Generate JWT token
			final String token = jwtUtil.generateToken(dto.getLoginId());
			res.addResult("token", token);
		}

		return res;
	}

	/**
	 * Forgot Password API: Sends password to user email
	 */
	@GetMapping("fp/{login}")
	public ORSResponse forgotPassword(@PathVariable String login, HttpServletRequest request) {

		ORSResponse res = new ORSResponse(true);
		UserDTO dto = baseService.forgotPassword(login);

		if (dto == null) {
			res.setSuccess(false);
			res.addMessage("Invalid Login Id");
		} else {
			res.setSuccess(	true);
			res.addMessage("Password has been sent to email id");
		}
		return res;
	}

	/**
	 * User Registration (Sign Up)
	 */
	@PostMapping("signUp")
	public ORSResponse signUp(@RequestBody @Valid UserRegistrationForm form, BindingResult bindingResult) {

		ORSResponse res = validate(bindingResult);

		if (!res.isSuccess()) {
			res.addMessage("Please fill following empty fields");
			return res;
		}

		UserDTO dto = baseService.findByLoginId(form.getLogin(), userContext);
		if (dto != null) {
			res.setSuccess(false);
			res.addMessage("Login Id already exists");
			return res;
		}

		 dto = new UserDTO();
		dto.setFirstName(form.getFirstName());
		dto.setLastName(form.getLastName());
		dto.setLoginId(form.getLogin());
		dto.setEmail(form.getLogin());
		dto.setGender(form.getGender());
		dto.setDob(form.getDob());
		dto.setPhone(form.getPhone());
		dto.setPassword(form.getPassword());
		dto.setStatus("Inactive");
		dto.setAlternateMobile(form.getAlternateMobile());
		dto.setRoleId(2L); // Default role ID for new users

		baseService.register(dto);

		res.setSuccess(true);
		res.addMessage("User Registered Successfully");
		return res;
	}

	/**
	 * Upload user profile picture
	 */
	@PostMapping("/profilePic/{userId}")
	public ORSResponse uploadPic(@PathVariable Long userId, @RequestParam("file") MultipartFile file,
			HttpServletRequest req) {

		UserDTO userDTO = baseService.findById(userId, userContext);

		AttachmentDTO doc = new AttachmentDTO(file);
		doc.setDescription("Profile picture");
		doc.setPath(req.getServletPath());
		doc.setUserId(userId);

		if (userDTO.getImageId() != null && userDTO.getImageId() > 0) {
			doc.setId(userDTO.getImageId());
		}

		Long imageId = attachmentService.save(doc, userContext);

		// Always update user's imageId
		userDTO.setImageId(imageId);
		baseService.update(userDTO, userContext);

		ORSResponse res = new ORSResponse(true);
		res.addResult("imageId", imageId);
		return res;
	}

	/**
	 * Get menu for logged-in user
	 */
	@GetMapping("menu")
	public ORSResponse menu(HttpSession session) {

		LinkedHashSet<MenuItem> menuBar = new LinkedHashSet<>();

		// Adding menu items
		MenuItem std = new MenuItem("Student", "/student");
		std.addSubmenu("Add Student", "/student");
		std.addSubmenu("Student List", "/studentlist");
		menuBar.add(std);

		MenuItem coll = new MenuItem("College", "/college");
		coll.addSubmenu("Add College", "/college");
		coll.addSubmenu("College List", "/collegelist");
		menuBar.add(coll);

		MenuItem mess = new MenuItem("Message", "/message");
		mess.addSubmenu("Add Message", "/message");
		mess.addSubmenu("Message List", "/messagelist");
		menuBar.add(mess);

		MenuItem user = new MenuItem("User", "/user");
		user.addSubmenu("Add User", "/user");
		user.addSubmenu("User List", "/userlist");
		menuBar.add(user);

		MenuItem marksheet = new MenuItem("Marksheet", "/marksheet");
		marksheet.addSubmenu("Add Marksheet", "/marksheet");
		marksheet.addSubmenu("Marksheet List", "/marksheetlist");
		marksheet.addSubmenu("Marksheet Merit List", "/marksheetmeritlist");
		marksheet.addSubmenu("Get Marksheet", "/getmarksheet");
		menuBar.add(marksheet);

		MenuItem role = new MenuItem("Role", "/role");
		role.addSubmenu("Add Role", "/role");
		role.addSubmenu("User Role", "/rolelist");
		menuBar.add(role);

		MenuItem course = new MenuItem("Course", "/course");
		course.addSubmenu("Add Course", "/course");
		course.addSubmenu("Course List", "/courselist");
		menuBar.add(course);

		MenuItem faculty = new MenuItem("Faculty", "/faculty");
		faculty.addSubmenu("Add Faculty", "/faculty");
		faculty.addSubmenu("Faculty List", "/facultylist");
		menuBar.add(faculty);

		MenuItem timetable = new MenuItem("TimeTable", "/timetable");
		timetable.addSubmenu("Add TimeTable", "/timetable");
		timetable.addSubmenu("TimeTable List", "/timeTablelist");
		menuBar.add(timetable);

		MenuItem subject = new MenuItem("Subject", "/subject");
		subject.addSubmenu("Add Subject", "/subject");
		subject.addSubmenu("Subject List", "/subjectlist");
		menuBar.add(subject);

		MenuItem current = new MenuItem("Current", "/current");
		current.addSubmenu("My Profile", "/myprofile");
		current.addSubmenu("Change Password", "/changepassword");
		current.addSubmenu("Java Doc", "");
		current.addSubmenu("Log out", "");
		menuBar.add(current);

		ORSResponse res = new ORSResponse(true);
		res.addData(menuBar);
		return res;
	}
}
