package com.rays.ctl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rays.common.BaseCtl;
import com.rays.common.DropdownList;
import com.rays.common.ORSResponse;
import com.rays.common.attachment.AttachmentDTO;
import com.rays.common.attachment.AttachmentServiceInt;
import com.rays.common.mail.EmailDTO;
import com.rays.common.mail.EmailServiceImpl;
import com.rays.dto.RoleDTO;
import com.rays.dto.UserDTO;
import com.rays.form.ChangePasswordForm;
import com.rays.form.ForgetPasswordForm;
import com.rays.form.MyProfileForm;
import com.rays.form.UserForm;
import com.rays.service.RoleServiceInt;
import com.rays.service.UserServiceInt;

/**
 * User Controller to handle User related operations.
 * Dipanshi Mukati
 */
@RestController
@RequestMapping(value = "User")
public class UserCtl extends BaseCtl<UserForm, UserDTO, UserServiceInt> {

    @Autowired
    RoleServiceInt roleService;

    @Autowired
    UserServiceInt userService;

    @Autowired
    AttachmentServiceInt attachmentService;

    @Autowired
    EmailServiceImpl emailSender;

    /**
     * Preloads role list for dropdown
     */
    @GetMapping("/preload")
    public ORSResponse preload() {
        ORSResponse res = new ORSResponse(true);
        RoleDTO dto = new RoleDTO();
        dto.setStatus(RoleDTO.ACTIVE);
        List<DropdownList> list = roleService.search(dto, userContext);
        res.addResult("roleList", list);
        return res;
    }

    /**
     * Logout the current session
     */
    @GetMapping("/logout")
    public ORSResponse logout(HttpServletRequest req, HttpServletResponse response) {
        ORSResponse res = new ORSResponse(true);
        HttpSession session = req.getSession();
        session.invalidate();
        res.addMessage("Logout Successfully");
        return res;
    }

    /**
     * Update profile details of the logged-in user
     */
    @PostMapping("myprofile")
    public ORSResponse myProfile(@RequestBody @Valid MyProfileForm form, BindingResult bindingResult) {
        ORSResponse res = validate(bindingResult);
        if (!res.isSuccess()) {
            return res;
        }
        UserDTO dto = baseService.findById(userContext.getUserId(), userContext);
        dto.setFirstName(form.getFirstName());
        dto.setLastName(form.getLastName());
        dto.setDob(form.getDob());
        dto.setPhone(form.getMobileNo());
        dto.setGender(form.getGender());

        baseService.update(dto, userContext);
        return res;
    }

    /**
     * Change password of logged-in user
     */
    @PostMapping("changepassword")
    public ORSResponse changePassword(@RequestBody @Valid ChangePasswordForm form, BindingResult bindingResult) {
        ORSResponse res = validate(bindingResult);
        if (!res.isSuccess()) {
            return res;
        }

        UserDTO changedDto = baseService.changePassword(form.getLoginId(), form.getOldPassword(), form.getNewPassword(), userContext);
        if (changedDto == null) {
            res.setSuccess(false);
            res.addMessage("Invalid old password");
            return res;
        }

        res.setSuccess(true);
        res.addMessage("Password has been changed");
        return res;
    }

    /**
     * Handle forgot password functionality
     */
    @PostMapping("forgetPassword")
    public ORSResponse forgetPassword(@RequestBody @Valid ForgetPasswordForm form, BindingResult bindingResult) {
        ORSResponse res = validate(bindingResult);

        UserDTO fDTO = baseService.forgotPassword(form.getLogin());
        if (fDTO == null) {
            res.setSuccess(false);
            res.addMessage("LoginId / Email not found.");
        } else {
            EmailDTO dto = new EmailDTO();
            dto.addTo(fDTO.getEmail());
            HashMap<String, String> params = new HashMap<>();
            params.put("code", "U-CP");
            dto.setMessageCode("U-CP", params);
            emailSender.send(dto, null);
            res.setSuccess(true);
            res.addMessage("Hello " + fDTO.getFirstName() + " " + fDTO.getLastName() + "! Your password has been sent on your email.");
        }
        return res;
    }

    /**
     * Upload profile picture for logged-in user
     */
    @PostMapping("/profilePic")
    public ORSResponse uploadPic(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        return uploadPic(userContext.getUserId(), file, req);
    }

    /**
     * Upload profile picture for given user id
     */
    @PostMapping("/profilePic/{userId}")
    public ORSResponse uploadPic(@PathVariable Long userId, @RequestParam("file") MultipartFile file, HttpServletRequest req) {
        UserDTO userDTO = baseService.findById(userId, userContext);
        AttachmentDTO doc = new AttachmentDTO(file);
        doc.setDescription("Profile picture");
        doc.setPath(req.getServletPath());
        doc.setUserId(userId);

        if (userDTO.getImageId() != null && userDTO.getImageId() > 0) {
            doc.setId(userDTO.getImageId());
        }

        Long imageId = attachmentService.save(doc, userContext);

        if (userDTO.getImageId() == null || userDTO.getImageId() == 0) {
            userDTO.setImageId(imageId);
            baseService.update(userDTO, userContext);
        }

        ORSResponse res = new ORSResponse();
        res.setSuccess(true);
        res.addResult("imageId", imageId);
        return res;
    }

    /**
     * Download profile picture of logged-in user
     */
    @GetMapping("/profilePic")
    public @ResponseBody void downloadPic(HttpServletResponse response) {
        downloadPic(userContext.getUserId(), response);
    }

    /**
     * Download profile picture by user id
     */
    @GetMapping("/profilePic/{userId}")
    public @ResponseBody void downloadPic(@PathVariable Long userId, HttpServletResponse response) {
        UserDTO userDTO = baseService.findById(userId, userContext);
        AttachmentDTO attachmentDTO = attachmentService.findById(userDTO.getImageId(), userContext);
        try {
            if (attachmentDTO != null) {
                response.setContentType(attachmentDTO.getType());
                OutputStream out = response.getOutputStream();
                out.write(attachmentDTO.getDoc());
                out.close();
            } else {
                response.getWriter().write("ERROR: File not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Upload a document for a user
     */
    @PostMapping("/doc/{userId}")
    public ORSResponse upload(@PathVariable Long userId, @RequestParam(required = false) String description,
                              @RequestParam("file") MultipartFile file, HttpServletRequest req) {
        AttachmentDTO doc = new AttachmentDTO(file);
        doc.setDescription(description);
        doc.setPath(req.getServletPath());
        doc.setUserId(userId);

        Long pk = attachmentService.save(doc, userContext);

        ORSResponse res = new ORSResponse();
        res.setSuccess(true);
        res.addResult("docId", pk);
        return res;
    }

    /**
     * Download document by id
     */
    @GetMapping("/doc/{id}")
    public @ResponseBody void download(@PathVariable Long id, HttpServletResponse response) {
        AttachmentDTO attachmentDTO = attachmentService.findById(id, userContext);
        try {
            if (attachmentDTO != null) {
                response.setContentType(attachmentDTO.getType());
                OutputStream out = response.getOutputStream();
                out.write(attachmentDTO.getDoc());
                out.close();
            } else {
                response.getWriter().write("ERROR: File not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get forgot password details by loginId
     */
    @GetMapping("forgotPassword/{loginId}")
    public ORSResponse myProfile(@PathVariable String loginId, HttpServletResponse response) {
        ORSResponse res = new ORSResponse();
        UserDTO userDto = baseService.forgotPassword(loginId);
        try {
            if (userDto != null) {
                res.addData(userDto);
                res.setSuccess(true);
            } else {
                res.setSuccess(false);
                res.addMessage("Login id does not exist");
            }
        } catch (Exception e) {
            res.addMessage(e.getMessage());
        }
        return res;
    }
	
	
}