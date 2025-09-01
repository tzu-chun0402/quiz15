package com.example.quiz15.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.quiz15.service.ifs.UserService;
import com.example.quiz15.vo.AddInfoReq;
import com.example.quiz15.vo.BasicRes;
import com.example.quiz15.vo.LoginReq;
import com.example.quiz15.vo.LoginRes;

import jakarta.validation.Valid;

@CrossOrigin 
@RestController
public class UserServiceController {

	@Autowired
	private UserService userService;

	@PostMapping("user/add_info")
	public BasicRes addInfo(@Valid @RequestBody AddInfoReq req) {
		return userService.addInfo(req);
	}

	@PostMapping("user/login")
	public LoginRes login(@Valid @RequestBody LoginReq req) {
		return userService.login(req);
	}

}
