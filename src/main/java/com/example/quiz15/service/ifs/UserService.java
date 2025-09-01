package com.example.quiz15.service.ifs;

import com.example.quiz15.vo.AddInfoReq;
import com.example.quiz15.vo.BasicRes;
import com.example.quiz15.vo.LoginReq;
import com.example.quiz15.vo.LoginRes;

public interface UserService {
	
	public BasicRes addInfo(AddInfoReq req);
	
	public LoginRes login(LoginReq req);

}
