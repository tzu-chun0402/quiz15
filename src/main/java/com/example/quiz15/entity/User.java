package com.example.quiz15.entity;

import com.example.quiz15.constants.ConstantsMessage;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "user")
public class User {

	@NotBlank(message = ConstantsMessage.NAME_ERROR)
	@Column(name = "name")
	private String name;

	@NotNull(message = ConstantsMessage.NOT_NULL)
	@Pattern(regexp = "09\\d{8}", message = ConstantsMessage.PHONE_FORMAT_ERROR)
	@Column(name = "phone")
	private String phone;

	@NotBlank(message = ConstantsMessage.EMAIL_ERROR)
	@Id
	@Column(name = "email")
	private String email;

	@Min(value = 0, message = ConstantsMessage.AGE_ERROR)
	@Column(name = "age")
	private int age;

	@NotBlank(message = ConstantsMessage.PASSWORD_ERROR)
	@Column(name = "password")
	private String password;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
