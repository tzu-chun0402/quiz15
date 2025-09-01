package com.example.quiz15.vo;

import java.util.List;

import com.example.quiz15.constants.ConstantsMessage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class FillinReq {

	@Min(value = 1, message = ConstantsMessage.QUIZ_ID_ERROR)
	private int quizId;

	@NotBlank(message = ConstantsMessage.EMAIL_ERROR)
	private String email;

	@Valid // �]�� vo �̭��� questionId �����ҡA�ҥH�n�[�W @Valid �~�|�������ҥͮ�
	private List<QuestionIdAnswerVo> questionAnswerVoList;

	public int getQuizId() {
		return quizId;
	}

	public void setQuizId(int quizId) {
		this.quizId = quizId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public List<QuestionIdAnswerVo> getQuestionAnswerVoList() {
		return questionAnswerVoList;
	}

	public void setQuestionAnswerVoList(List<QuestionIdAnswerVo> questionAnswerVoList) {
		this.questionAnswerVoList = questionAnswerVoList;
	}

}
