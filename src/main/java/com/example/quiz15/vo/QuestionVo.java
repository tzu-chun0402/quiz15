package com.example.quiz15.vo;

import java.util.List;

import com.example.quiz15.constants.ConstantsMessage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// �@�� QuestionVo �N��@�D
public class QuestionVo {

	// ���ݩʤ��ˬd
	// QuestionVo �Q�ϥΦb QuizCreateReq �M QuestionRes 2�����O���A
	// ��ϥΦb QuizCreateReq �ɦ��ݩʵL�@�ΡA�]�ݨ��s���O�۰ʥͦ����y�����F
	// ��ϥΦb QuestionRes �ɫh�����n���ݨ��s������
	private int quizId;

	@Min(value = 1, message = ConstantsMessage.QUESTION_ID_ERROR)
	private int questionId;

	@NotBlank(message = ConstantsMessage.QUESTION_ERROR)
	private String question;

	@NotBlank(message = ConstantsMessage.QUESTION_TYPE_ERROR)
	private String type;

	private boolean required;

	// ���ˬd�A�]��²���D���|���ﶵ
	private List<String> options;

	public QuestionVo() {
		super();
	}

	public QuestionVo(int questionId, String question, String type, boolean required, List<String> options) {
		super();
		this.questionId = questionId;
		this.question = question;
		this.type = type;
		this.required = required;
		this.options = options;
	}

	public QuestionVo(int quizId, int questionId, String question, String type, boolean required,
			List<String> options) {
		super();
		this.quizId = quizId;
		this.questionId = questionId;
		this.question = question;
		this.type = type;
		this.required = required;
		this.options = options;
	}

	public int getQuestionId() {
		return questionId;
	}

	public void setQuestionId(int questionId) {
		this.questionId = questionId;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public List<String> getOptions() {
		return options;
	}

	public void setOptions(List<String> options) {
		this.options = options;
	}

	public int getQuizId() {
		return quizId;
	}

	public void setQuizId(int quizId) {
		this.quizId = quizId;
	}

}
