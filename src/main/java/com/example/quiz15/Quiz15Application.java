package com.example.quiz15;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

//�]�����ϥ� spring-boot-starter-security ���̿�A�n�ư��w�]���򥻦w���ʳ]�w(�b�K�n�J����)
//�ư��b�K�n�J���ҴN�O�[�W exclude = SecurityAutoConfiguration.class
//�����᭱�Y���h�� class �ɡA�N�n�Τj�A���A�@�Ӯɤj�A���i���i�L
//�h�� class �O�γr��(,)�Ϲj
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class Quiz15Application {

	public static void main(String[] args) {
		SpringApplication.run(Quiz15Application.class, args);
	}

}
