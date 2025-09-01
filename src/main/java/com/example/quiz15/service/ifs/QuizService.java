package com.example.quiz15.service.ifs;

import com.example.quiz15.vo.BasicRes;
import com.example.quiz15.vo.FeedbackRes;
import com.example.quiz15.vo.FeedbackUserRes;
import com.example.quiz15.vo.FillinReq;
import com.example.quiz15.vo.QuestionRes;
import com.example.quiz15.vo.QuizCreateReq;
import com.example.quiz15.vo.QuizUpdateReq;
import com.example.quiz15.vo.SearchReq;
import com.example.quiz15.vo.SearchRes;
import com.example.quiz15.vo.StatisticsRes;

public interface QuizService {
	
	public BasicRes create(QuizCreateReq req) throws Exception;
	
	public BasicRes update(QuizUpdateReq req) throws Exception;
	
	public SearchRes getAllQuizs();
	
	public QuestionRes getQuizsByQuizId(int quizId);
	
	public SearchRes search(SearchReq req);
	
	public BasicRes delete(int quizId) throws Exception;
	
	public BasicRes fillin(FillinReq req) throws Exception;
	
	public FeedbackUserRes feedbackUserList(int quizId);
	
	public FeedbackRes feedback(int quizId, String email);
	
	public StatisticsRes statistics(int quizId);

}
