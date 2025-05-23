package com.rays.common;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.rays.dto.UserDTO;

/**
 * Base controller class containing common REST APIs (get, search, save,
 * delete).
 * 
 * Dipanshi Mukati
 */
public abstract class BaseCtl<F extends BaseForm, T extends BaseDTO, S extends BaseServiceInt<T>> {
// Operation constants
	protected static final String OP_SAVE = "Save";
	protected static final String OP_NEW = "New";
	protected static final String OP_DELETE = "Delete";
	protected static final String OP_CANCEL = "Cancel";
	protected static final String OP_ERROR = "Error";
	protected static final String OP_NEXT = "Next";
	protected static final String OP_PREVIOUS = "Previous";
	protected static final String OP_LOGOUT = "Logout";
	protected static final String OP_GO = "Go";
	protected static final String OP_GET = "Get";

	@Autowired
	protected S baseService;

	@Value("${page.size}")
	private int pageSize = 0;

	/**
	 * Context of logged-in user.
	 */
	protected UserContext userContext = null;

	/**
	 * Set user context from session.
	 * 
	 * @param session
	 */
	@ModelAttribute
	public void setUserContext(HttpSession session) {
		userContext = (UserContext) session.getAttribute("userContext");
		if (userContext == null) {
			UserDTO dto = new UserDTO();
			dto.setLoginId("root@sunilos.com");
			dto.setFirstName("demo firstName");
			dto.setLastName("demo lastName");
			dto.setOrgId(0L);
			dto.setRoleId(1L);
			dto.setOrgName("root");
			userContext = new UserContext(dto);
		}
	}

	/**
	 * Default GET mapping.
	 * 
	 * @return ORSResponse
	 */
	@GetMapping
	public ORSResponse get() {
		ORSResponse res = new ORSResponse(true);
		res.addData("I am okay " + this.getClass() + " --" + new Date());
		return res;
	}

	/**
	 * Get entity by primary key ID.
	 * 
	 * @param id
	 * @return ORSResponse
	 */
	@GetMapping("get/{id}")
	public ORSResponse get(@PathVariable long id) {
		ORSResponse res = new ORSResponse(true);
		T dto = baseService.findById(id, userContext);
		if (dto != null) {
			res.addData(dto);
		} else {
			res.setSuccess(false);
			res.addMessage("Record not found");
		}
		return res;
	}

	/**
	 * Delete entity by primary key ID.
	 * 
	 * @param id
	 * @return ORSResponse
	 */
	@GetMapping("delete/{id}")
	public ORSResponse delete(@PathVariable long id) {
		ORSResponse res = new ORSResponse(true);
		try {
			T dto = baseService.delete(id, userContext);
			res.addData(dto);
		} catch (Exception e) {
			res.setSuccess(false);
			res.addMessage(e.getMessage());
		}
		return res;
	}

	/**
	 * Delete multiple records by IDs.
	 * 
	 * @param ids
	 * @param pageNo
	 * @param form
	 * @return ORSResponse
	 */
	@PostMapping("deleteMany/{ids}")
	public ORSResponse deleteMany(@PathVariable String[] ids, @RequestParam("pageNo") String pageNo,
			@RequestBody F form) {
		ORSResponse res = new ORSResponse(true);
		try {
			for (String id : ids) {
				baseService.delete(Long.parseLong(id), userContext);
			}
			T dto = (T) form.getDto();
			List<T> list = baseService.search(dto, Integer.parseInt(pageNo), pageSize, userContext);
			res.addData(list);
			res.setSuccess(true);
			res.addMessage("Records Deleted Successfully");
		} catch (Exception e) {
			res.setSuccess(false);
			res.addMessage(e.getMessage());
		}
		return res;
	}

	/**
	 * Search entities (with or without pagination).
	 * 
	 * @param form
	 * @return ORSResponse
	 */
	@RequestMapping(value = "/search", method = { RequestMethod.GET, RequestMethod.POST })
	public ORSResponse search(@RequestBody F form) {
		String operation = form.getOperation();
		int pageNo = form.getPageNo();

		if (OP_NEXT.equals(operation)) {
			pageNo++;
		} else if (OP_PREVIOUS.equals(operation)) {
			pageNo--;
		}

		pageNo = Math.max(pageNo, 0);
		form.setPageNo(pageNo);

		T dto = (T) form.getDto();
		ORSResponse res = new ORSResponse(true);
		res.addData(baseService.search(dto, pageNo, pageSize, userContext));
		return res;
	}

	/**
	 * Search with specific page number.
	 * 
	 * @param form
	 * @param pageNo
	 * @return ORSResponse
	 */
	@RequestMapping(value = "/search/{pageNo}", method = { RequestMethod.GET, RequestMethod.POST })
	public ORSResponse search(@RequestBody F form, @PathVariable int pageNo) {
		pageNo = Math.max(pageNo, 0);//req ka data form me hold karta h

		T dto = (T) form.getDto();
		ORSResponse res = new ORSResponse(true);
		res.addData(baseService.search(dto, pageNo, pageSize, userContext));

		List<T> nextList = baseService.search(dto, pageNo + 1, pageSize, userContext);
		res.addResult("nextList", nextList.size());

		return res;
	}

	/**
	 * Save or update entity.
	 * 
	 * @param form
	 * @param bindingResult
	 * @return ORSResponse
	 */
	@PostMapping("/save")
	public ORSResponse save(@RequestBody @Valid F form, BindingResult bindingResult) {
		ORSResponse res = validate(bindingResult);

		if (!res.isSuccess()) {
			return res;
		}
		try {
			T dto = (T) form.getDto();
			if (dto.getId() != null && dto.getId() > 0) {
				T existDto = baseService.findByUniqueKey(dto.getUniqueKey(), dto.getUniqueValue(), userContext);
				if (existDto != null && !dto.getId().equals(existDto.getId())) {
					res.addMessage(dto.getLabel() + " already exist");
					res.setSuccess(false);
					res.addData(dto.getId());
					return res;
				}
				baseService.update(dto, userContext);
			} else {
				if (dto.getUniqueKey() != null && !dto.getUniqueKey().isEmpty()) {
					T existDto = baseService.findByUniqueKey(dto.getUniqueKey(), dto.getUniqueValue(), userContext);
					if (existDto != null) {
						res.addMessage(dto.getLabel() + " already exist");
						res.setSuccess(false);
						return res;
					}
				}
				baseService.add(dto, userContext);
			}
			res.addData(dto.getId());
		} catch (Exception e) {
			res.setSuccess(false);
			res.addMessage(e.getMessage());
		}
		return res;
	}

	/**
	 * Validate form input errors.
	 * 
	 * @param bindingResult
	 * @return ORSResponse
	 */
	public ORSResponse validate(BindingResult bindingResult) {
		ORSResponse res = new ORSResponse(true);
		if (bindingResult.hasErrors()) {
			res.setSuccess(false);
			Map<String, String> errors = new HashMap<>();
			List<FieldError> list = bindingResult.getFieldErrors();
			list.forEach(e -> 
			errors.put(e.getField(), e.getDefaultMessage()));
		
			res.addInputErrors(errors);
		}
		return res;
	}
}
