package com.example.quiz15.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.quiz15.constants.ResCodeMessage;
import com.example.quiz15.dao.UserDao;
import com.example.quiz15.entity.User;
import com.example.quiz15.service.ifs.UserService;
import com.example.quiz15.vo.AddInfoReq;
import com.example.quiz15.vo.BasicRes;
import com.example.quiz15.vo.LoginReq;
import com.example.quiz15.vo.LoginRes;

@Service
public class UserServiceImpl implements UserService {

	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Autowired
	private UserDao userDao;

	@Override
	public BasicRes addInfo(AddInfoReq req) {
		// req ���Ѽ��ˬd�w�g�b���O User ���z�L Validation ����
		// 1. �ˬd�b���O�_�w�s�b
		// �z�L PK email �h���o count �ơA�u�|�o�� 0 �� 1
		int count = userDao.getCountByEmail(req.getEmail());
		if (count == 1) { // email �O PK�A�Y count = 1 ��ܱb���w�s�b
			return new BasicRes(ResCodeMessage.EMAIL_EXISTS.getCode(), //
					ResCodeMessage.EMAIL_EXISTS.getMessage());
		}
		// 2. �s�W��T
		try {
			userDao.addInfo(req.getName(), req.getPhone(), req.getEmail(), //
					req.getAge(), encoder.encode(req.getPassword()));
			return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
					ResCodeMessage.SUCCESS.getMessage());
		} catch (Exception e) {
			return new BasicRes(ResCodeMessage.ADD_INFO_ERROR.getCode(), //
					ResCodeMessage.ADD_INFO_ERROR.getMessage());
		}
	}

	@Override
	public LoginRes login(LoginReq req) {
		User user = userDao.getByEmail(req.getEmail());
		if (user == null) {
			return new LoginRes(ResCodeMessage.NOT_FOUND.getCode(), //
					ResCodeMessage.NOT_FOUND.getMessage(), null);
		}

		if (!encoder.matches(req.getPassword(), user.getPassword())) {
			return new LoginRes(ResCodeMessage.PASSWORD_MISMATCH.getCode(), //
					ResCodeMessage.PASSWORD_MISMATCH.getMessage(), null);
		}
		return new LoginRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), user);
	}

}
