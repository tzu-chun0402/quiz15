package com.example.quiz15.vo;

import com.example.quiz15.entity.User;

public class LoginRes extends BasicRes {

	private User user;

	public LoginRes() {
	}

	public LoginRes(int code, String message, User user) {
		super(code, message);
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
