package com.example.quiz15.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quiz15.constants.QuestionType;
import com.example.quiz15.constants.ResCodeMessage;
import com.example.quiz15.dao.FillinDao;
import com.example.quiz15.dao.QuestionDao;
import com.example.quiz15.dao.QuizDao;
import com.example.quiz15.entity.Question;
import com.example.quiz15.entity.Quiz;
import com.example.quiz15.service.ifs.QuizService;
import com.example.quiz15.vo.BasicRes;
import com.example.quiz15.vo.FeedbackRes;
import com.example.quiz15.vo.FeedbackUserRes;
import com.example.quiz15.vo.FillinReq;
import com.example.quiz15.vo.OptionCountVo;
import com.example.quiz15.vo.QuestionAnswerDto;
import com.example.quiz15.vo.QuestionAnswerVo;
import com.example.quiz15.vo.QuestionIdAnswerVo;
import com.example.quiz15.vo.QuestionRes;
import com.example.quiz15.vo.QuestionVo;
import com.example.quiz15.vo.QuizCreateReq;
import com.example.quiz15.vo.QuizUpdateReq;
import com.example.quiz15.vo.SearchReq;
import com.example.quiz15.vo.SearchRes;
import com.example.quiz15.vo.StatisticsRes;
import com.example.quiz15.vo.StatisticsVo;
import com.example.quiz15.vo.UserVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class QuizServiceImpl implements QuizService {

	// slf4j
	private Logger logger = LoggerFactory.getLogger(getClass());

	// 提供 類別(或Json 格式)與物件之間的轉換
	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private QuizDao quizDao;

	@Autowired
	private QuestionDao questionDao;

	@Autowired
	private FillinDao fillinDao;

	/**
	 * @throws Exception
	 * @Transactional: 事務</br>
	 *                 1. 當一個方法中執行多個 Dao 時(跨表或是同一張表寫多筆資料)，這些所有的資料應該都要算同一次的行為，
	 *                 所以這些資料要嘛全部成功寫入，不然就全部寫入失敗</br>
	 *                 2. @Transactional 有效回朔的異常預設是 RunTimeException，若發生的異常不是
	 *                 RunTimeException
	 *                 或其子類別的異常類型，資料皆不會回朔，因此想要讓只要發生任何一種異常時資料都要可以回朔，可以
	 *                 將 @Transactional 的有效範圍從 RunTimeException 提高至 Exception
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes create(QuizCreateReq req) throws Exception {
		// 參數檢查已透過 @Valid 驗證了
		try {
			// 檢查日期: 使用排除法
			BasicRes checkRes = checkDate(req.getStartDate(), req.getEndDate());
			if (checkRes.getCode() != 200) { // 不等於 200 表示檢查出有錯誤
				return checkRes;
			}
			// 新增問卷
			quizDao.insert(req.getName(), req.getDescription(), req.getStartDate(), //
					req.getEndDate(), req.isPublished());
			// 新增完問卷後，取得問卷流水號
			// 雖然因為 @Transactional 尚未將資料提交(commit)進資料庫，但實際上SQL語法已經執行完畢，
			// 依然可以取得對應的值
			int quizId = quizDao.getMaxQuizId();
			// 新增問題
			// 取出問卷中的所有問題
			List<QuestionVo> questionVoList = req.getQuestionList();
			// 處理每一題問題
			for (QuestionVo vo : questionVoList) {
				// 檢查題目類型與選項
				checkRes = checkQuestionType(vo);
				// 呼叫方法 checkQuestionType 得到的 res 若是 成功，表示檢查都沒問題，
				// 因為方法中檢查到最後都沒問題時是回傳 成功
				if (checkRes.getCode() != 200) {
//					return checkRes;
					// 因為前面已經執行了 quizDao.insert 了，所以這邊要拋出 Exception
					// 才會讓 @Transactional 生效
					throw new Exception(checkRes.getMessage());
				}
				// 因為 MySQL 沒有 List 的資料格式，所以要把 options 資料格式 從 List<String> 轉成 String
				List<String> optionsList = vo.getOptions();
				String str = mapper.writeValueAsString(optionsList);
				// 要記得設定 quizId
				questionDao.insert(quizId, vo.getQuestionId(), vo.getQuestion(), //
						vo.getType(), vo.isRequired(), str);
			}
			return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
					ResCodeMessage.SUCCESS.getMessage());
		} catch (Exception e) {
			// 不能 return BasicRes 而是要將發生的異常拋出去，這樣 @Transaction 才會生效
			throw e;
		}
	}

	private BasicRes checkQuestionType(QuestionVo vo) {
		// 1. 檢查 type 是否是規定的類型
		String type = vo.getType();
		// 假設 從 vo 取出的 type 不符合定義的3種類型的其中一種，就返回錯誤訊息
		if (!(type.equalsIgnoreCase(QuestionType.SINGLE.getType())//
				|| type.equalsIgnoreCase(QuestionType.MULTI.getType())//
				|| type.equalsIgnoreCase(QuestionType.TEXT.getType()))) {
			return new BasicRes(ResCodeMessage.QUESTION_TYPE_ERROR.getCode(), //
					ResCodeMessage.QUESTION_TYPE_ERROR.getMessage());
		}
		// 2. type 是單選或多選的時候，選項(options)至少要有2個
		// 假設 type 不等於 TEXT --> 就表示 type 是單選或是多選
		if (!type.equalsIgnoreCase(QuestionType.TEXT.getType())) {
			// 單選或多選時，選項至少要有2個
			if (vo.getOptions().size() < 2) {
				return new BasicRes(ResCodeMessage.OPTIONS_INSUFFICIENT.getCode(), //
						ResCodeMessage.OPTIONS_INSUFFICIENT.getMessage());
			}
		} else { // else --> type 是 text --> 選項應該是 null 或是 size = 0
			// 假設 選項不是 null 或 選項的 List 有值
			if (vo.getOptions() != null && vo.getOptions().size() > 0) {
				return new BasicRes(ResCodeMessage.TEXT_HAS_OPTIONS_ERROR.getCode(), //
						ResCodeMessage.TEXT_HAS_OPTIONS_ERROR.getMessage());
			}
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	private BasicRes checkDate(LocalDate startDate, LocalDate endDate) {
		// 1. 開始日期不能比結束日期晚 2. 開始日期不能比當前創建日期早
		// 判斷式: 假設 開始日期比結束日期晚 或 開始日期比當前日期早 --> 回錯誤訊息
		// LocalDate.now() --> 取得當前的日期
		if (startDate.isAfter(endDate) //
				|| startDate.isBefore(LocalDate.now())) {
			return new BasicRes(ResCodeMessage.DATE_FORMAT_ERROR.getCode(), //
					ResCodeMessage.DATE_FORMAT_ERROR.getMessage());
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	private BasicRes checkStatus(LocalDate startDate, boolean isPublished) {
		// 允許問卷修改的狀態條件: 1. 尚未發佈 2.已發佈+尚未開始
		// 依照上面條件，程式的寫法如下
		// if(!isPublished || (isPublished && startDate.isAfter(LocalDate.now())))
		// 但是因為2個條件式是用 OR (||) 串接，表示只要一個條件成立就會返回成功
		// 因此只要有比較到 || 後面的條件式，隱含著 isPublished = true，
		// 上面的 if 條件式可修改成如下
		if (!isPublished || startDate.isAfter(LocalDate.now())) {
			// 返回成功表示問卷允許被修改
			return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
					ResCodeMessage.SUCCESS.getMessage());
		}
		return new BasicRes(ResCodeMessage.QUIZ_CANNOT_BE_EDITED.getCode(), //
				ResCodeMessage.QUIZ_CANNOT_BE_EDITED.getMessage());
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes update(QuizUpdateReq req) throws Exception {
		// 參數檢查已透過 @Valid 驗證了

		// 更新是對已存在問卷進行修改
		try {
			// 1. 檢查 quizId 是否存在
			int quizId = req.getQuizId();
			// int count = quizDao.getCountByQuizId(quizId);
			// 不使用 count 數而是取出整筆資料主要是因為後續還會使用到資料庫中的資料
			Quiz quiz = quizDao.getById(quizId);
			if (quiz == null) {
				return new BasicRes(ResCodeMessage.NOT_FOUND.getCode(), //
						ResCodeMessage.NOT_FOUND.getMessage());
			}
			// 2. 檢查日期
			BasicRes checkRes = checkDate(req.getStartDate(), req.getEndDate());
			if (checkRes.getCode() != 200) { // 不等於 200 表示檢查出有錯誤
				return checkRes;
			}
			// 3. 檢查原本的問卷狀態(相關欄位的值是存在於DB中)是否可被更新
			checkRes = checkStatus(quiz.getStartDate(), quiz.isPublished());
			if (checkRes.getCode() != 200) { // 不等於 200 表示問卷不允許被更新
				return checkRes;
			}
			// 4. 更新問卷
			int updateRes = quizDao.update(quizId, req.getName(), req.getDescription(), //
					req.getStartDate(), req.getEndDate(), req.isPublished());
			if (updateRes != 1) { // 表是資料沒更新成功
				return new BasicRes(ResCodeMessage.QUIZ_UPDATE_FAILED.getCode(), //
						ResCodeMessage.QUIZ_UPDATE_FAILED.getMessage());
			}
			// 5. 刪除同一張問卷的所有問題
			questionDao.deleteByQuizId(quizId);
			// 6. 檢查問題
			List<QuestionVo> questionVoList = req.getQuestionList();
			for (QuestionVo vo : questionVoList) {
				// 檢查題目類型與選項
				checkRes = checkQuestionType(vo);
				// 方法中檢查到最後都沒問題時是回傳 成功
				if (checkRes.getCode() != 200) {
					// 因為前面已經執行了 quizDao.insert 了，所以這邊要拋出 Exception
					// 才會讓 @Transactional 生效
					throw new Exception(checkRes.getMessage());
				}
				// 因為 MySQL 沒有 List 的資料格式，所以要把 options 資料格式 從 List<String> 轉成 String
				List<String> optionsList = vo.getOptions();
				String str = mapper.writeValueAsString(optionsList);
				// 要記得設定 quizId
				questionDao.insert(quizId, vo.getQuestionId(), vo.getQuestion(), //
						vo.getType(), vo.isRequired(), str);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			// 不能 return BasicRes 而是要將發生的異常拋出去，這樣 @Transaction 才會生效
			throw e;
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	@Override
	public SearchRes getAllQuizs() {
		List<Quiz> list = quizDao.getAll();
		return new SearchRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), list);
	}

	@Override
	public QuestionRes getQuizsByQuizId(int quizId) {
		if (quizId <= 0) {
			return new QuestionRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		List<QuestionVo> questionVoList = new ArrayList<>();
		List<Question> list = questionDao.getQuestionsByQuizId(quizId);
		// 把每題選項的資型態從 String 轉換成 List<String>
		for (Question item : list) {
			String str = item.getOptions();
			try {
				List<String> optionList = mapper.readValue(str, new TypeReference<>() {
				});
				// 將從DB取得的每一筆資料(Question item) 的每個欄位值放在 QuestionVo中，以便返回給使用者
				// Question 和 QuestionVo 的差別在於 選項 的資料型態
				QuestionVo vo = new QuestionVo(item.getQuizId(), item.getQuestionId(), //
						item.getQuestion(), item.getType(), item.isRequired(), optionList);
				// 把每個 vo 放到 questionVoList 中
				questionVoList.add(vo);
			} catch (Exception e) {
				// 這邊不寫 throw e 是因為這次方法中沒有使用 @Transactional，不影響返回結果
				return new QuestionRes(ResCodeMessage.OPTIONS_TRANSFER_ERROR.getCode(), //
						ResCodeMessage.OPTIONS_TRANSFER_ERROR.getMessage());
			}
		}
		return new QuestionRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), questionVoList);
	}

	@Override
	public SearchRes search(SearchReq req) {
		// 轉換 req 的值
		// 若 quizName 是 null，轉成空字串
		String quizName = req.getQuizName();
		if (quizName == null) {
			quizName = "";
		} else { // 多餘的，不需要寫，但為了理解下面的3元運算子而寫
			quizName = quizName;
		}
		// 3元運算子
		// 格式: 變數名稱 = 條件判斷式 ? 判斷式結果為 true 時要賦予的值 : 判斷式結果為 false 時要賦予的值;
		quizName = quizName == null ? "" : quizName;
		// 上面的程式碼可以只用下面一行來取得值
		String quizName1 = req.getQuizName() == null ? "" : req.getQuizName();
		// =========================================
		// 轉換 開始時間 --> 若沒有給開始日期 --> 給定一個很早的時間
		LocalDate startDate = req.getStartDate() == null ? LocalDate.of(1970, 1, 1) //
				: req.getStartDate();

		LocalDate endDate = req.getEndDate() == null ? LocalDate.of(2999, 12, 31) //
				: req.getEndDate();
		List<Quiz> list = new ArrayList<>();
		if (req.isPublished()) {
			list = quizDao.getAllPublished(quizName, startDate, endDate);
		} else {
			list = quizDao.getAll(quizName, startDate, endDate);
		}
		return new SearchRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), list);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes delete(int quizId) throws Exception {
		if (quizId <= 0) {
			return new BasicRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		Quiz quiz = quizDao.getById(quizId);
		// 要判斷是否為 null，若不判斷且取得的值是 null 時，後續使用方法會報錯
		if (quiz == null) {
			return new BasicRes(ResCodeMessage.NOT_FOUND.getCode(), //
					ResCodeMessage.NOT_FOUND.getMessage());
		}
		// 檢查問卷狀態是否可被更新
		BasicRes checkRes = checkStatus(quiz.getStartDate(), quiz.isPublished());
		if (checkRes.getCode() != 200) { // 不等於 200 表示問卷不允許被更新
			return checkRes;
		}
		try {
			quizDao.deleteById(quizId);
			questionDao.deleteByQuizId(quizId);
		} catch (Exception e) {
			// 不能 return BasicRes 而是要將發生的異常拋出去，這樣 @Transaction 才會生效
			throw e;
		}
		return null;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes fillin(FillinReq req) throws Exception {
		// 檢查填寫的問卷(quiz)
		// 檢查 1. 是否以發佈 2. 當下的日期是否可以填寫(當天是否介於 開始日期和結束日期 之間)
		int count = quizDao.selectCountById(req.getQuizId(), LocalDate.now());
		if (count != 1) {
			return new BasicRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		// 檢查同一個 email 只能寫同一份問卷
		count = fillinDao.selectCountByQuizIdAndEmail(req.getQuizId(), req.getEmail());
		if (count != 0) {
			return new BasicRes(ResCodeMessage.EMAIL_DUPLICATED.getCode(), //
					ResCodeMessage.EMAIL_DUPLICATED.getMessage());
		}
		// 檢查題目
		// 檢查 1. 必填但沒有答案 2. 單選但有多個答案 3. 答案和選項是一樣(答案必須是選項之一)
		// 取得一張問卷所有題目
		List<Question> questionList = questionDao.getQuestionsByQuizId(req.getQuizId());
		List<QuestionIdAnswerVo> questionAnswerVoList = req.getQuestionAnswerVoList();
		// 將問題編號和回答轉換成 Map，就是將 QuestionAnswerVo 裡面的2個屬性轉成 Map
		Map<Integer, List<String>> answerMap = new HashMap<>();
		for (QuestionIdAnswerVo vo : questionAnswerVoList) {
			answerMap.put(vo.getQuestionId(), vo.getAnswerList());
		}
		// 檢查每一題
		for (Question question : questionList) {
			int questionId = question.getQuestionId();
			String type = question.getType();
			boolean required = question.isRequired();
			// 1.檢查必填但沒有答案 --> 必填但 questionId 沒有在 answerMap 的 key 裡面
			if (required && !answerMap.containsKey(questionId)) {
				return new BasicRes(ResCodeMessage.ANSWER_REQUIRED.getCode(), //
						ResCodeMessage.ANSWER_REQUIRED.getMessage());
			}
			// 2. 檢查單選但有多個答案
			if (type.equalsIgnoreCase(QuestionType.SINGLE.getType())) {
				List<String> answerList = answerMap.get(questionId);
				if (answerList.size() > 1) {
					return new BasicRes(ResCodeMessage.QUESTION_TYPE_IS_SINGLE.getCode(), //
							ResCodeMessage.QUESTION_TYPE_IS_SINGLE.getMessage());
				}
			}
			// 簡答題沒有選項，跳過
			if (type.equalsIgnoreCase(QuestionType.TEXT.getType())) {
				continue;
			}
			// 3. 比對該題的答案和選項是一樣(答案必須是選項之一)
			String optionsStr = question.getOptions();
			List<String> answerList = answerMap.get(questionId);
			for (String answer : answerList) {
				// 將每個答案比對是否被包含在選項字串中
				if (!optionsStr.contains(answer)) {
					return new BasicRes(ResCodeMessage.OPTION_ANSWER_MISMATCH.getCode(), //
							ResCodeMessage.OPTION_ANSWER_MISMATCH.getMessage());
				}
			}
		}
		// 存資料: 一提存成一筆資料
		for (QuestionIdAnswerVo vo : questionAnswerVoList) {
			// 把 answerList 轉成字串型態
			try {
				String str = mapper.writeValueAsString(vo.getAnswerList());
				fillinDao.insert(req.getQuizId(), vo.getQuestionId(), req.getEmail(), //
						str, LocalDate.now());
			} catch (Exception e) {
				throw e;
			}
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	public BasicRes fillin_test(FillinReq req) {
		// 檢查填寫的問卷(quiz)
		// 檢查 1. 是否以發佈 2. 當下的日期是否可以填寫(當天是否介於 開始日期和結束日期 之間)
		int count = quizDao.selectCountById(req.getQuizId(), LocalDate.now());
		if (count != 1) {
			return new BasicRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}

		// 檢查題目
		// 檢查 1. 必填但沒有答案 2. 單選但有多個答案 3. 答案和選項是一樣(答案必須是選項之一)
		// 取得一張問卷所有題目
		List<Question> questionList = questionDao.getQuestionsByQuizId(req.getQuizId());
		List<QuestionIdAnswerVo> questionAnswerVoList = req.getQuestionAnswerVoList();
		// questionAnswerVoList 的 size 可能會比 questionList 的 size 少，因為有可能是非必填而沒做答
		// 因為要知道每一題是否必填、單多選，這樣才能拿填寫的答案來比對
		// --> 所以 questionList 要當成外層迴圈
		
		// 先把必填題的 questionId 放到一個 List 中
		List<Integer> questionIdList = new ArrayList<>();
		for (Question question : questionList) {
			if (question.isRequired()) {
				questionIdList.add(question.getQuestionId());
			}
		}

		for (Question question : questionList) {
			int questionId = question.getQuestionId();
			String type = question.getType();
			boolean required = question.isRequired();
			// 該題是必填 --> 檢查 VoList 中是否有該題的編號存在
			for (QuestionIdAnswerVo vo : questionAnswerVoList) {
				int voQuestionId = vo.getQuestionId();
				// 該題必填但題目編號不包含在 questionIdList，回傳錯誤
				if (required && !questionIdList.contains(voQuestionId)) {
					return new BasicRes(ResCodeMessage.ANSWER_REQUIRED.getCode(), //
							ResCodeMessage.ANSWER_REQUIRED.getMessage());
				}
				List<String> answerList = vo.getAnswerList();
				// 檢查相同的 questionId，該題是必填但沒有答案 --> 回傳錯誤
				if (questionId == voQuestionId && required //
						// CollectionUtils.isEmpty 有判斷到 null
						// QuestionAnswerVO 中的 answerList 有給定新的預設值，沒有 mapping 到時會是一個空的 List
						&& answerList.isEmpty()) {
					return new BasicRes(ResCodeMessage.ANSWER_REQUIRED.getCode(), //
							ResCodeMessage.ANSWER_REQUIRED.getMessage());
				}
				// 檢查相同的 questionId，單選但有多個答案 --> 回傳錯誤
				if (questionId == voQuestionId && type == QuestionType.SINGLE.getType() //
						&& answerList.size() > 1) {
					return new BasicRes(ResCodeMessage.QUESTION_TYPE_IS_SINGLE.getCode(), //
							ResCodeMessage.QUESTION_TYPE_IS_SINGLE.getMessage());
				}
			}
		}
		return null;
	}

	@Override
	public FeedbackUserRes feedbackUserList(int quizId) {
		if (quizId <= 0) {
			return new FeedbackUserRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		List<UserVo> userVoList = fillinDao.selectUserVoList(quizId);
		return new FeedbackUserRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), quizId, userVoList);
	}

	@Override
	public FeedbackRes feedback(int quizId, String email) {
		if (quizId <= 0) {
			return new FeedbackRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		List<QuestionAnswerDto> dtoList = fillinDao.selectQuestionAnswerList(quizId, email);
		List<QuestionAnswerVo> voList = new ArrayList<>();
		// 將QuestionAnswerDto 轉成 QuestionAnswerVo
		for (QuestionAnswerDto dto : dtoList) {
			List<String> answerList;
			try {
				if (dto.getAnswerStr() == null || dto.getAnswerStr().isBlank()) {
					// 非必填題沒有填，給空陣列
					answerList = new ArrayList<>();
				} else {
					answerList = mapper.readValue(dto.getAnswerStr(), new TypeReference<List<String>>() {
					});
				}
			} catch (Exception e) {
				// parse 失敗也給空陣列，而不是直接回錯
				answerList = new ArrayList<>();
			}

			QuestionAnswerVo vo = new QuestionAnswerVo(dto.getQuestionId(), dto.getQuestion(), dto.getType(),
					dto.isRequired(), answerList);
			voList.add(vo);
		}
		// 原寫法
//		// 將QuestionAnswerDto 轉成 QuestionAnswerVo
//		for (QuestionAnswerDto dto : dtoList) {
//			// 將 dto 中的 answerStr 轉換成List<String>
//			try {
//				List<String> answerList = mapper.readValue(dto.getAnswerStr(), new TypeReference<>() {});
//				// 一對一將 dto 的資料設定到 vo 中
//				QuestionAnswerVo vo = new QuestionAnswerVo(dto.getQuestionId(), //
//						dto.getQuestion(), dto.getType(), dto.isRequired(), answerList);
//				voList.add(vo);
//			} catch (Exception e) {
//				// 不需要 throw 因為沒有使用 @Transactional，就只有取資料而已，所以可以 return 自定義
//				return new FeedbackRes(ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getCode(), //
//						ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getMessage());
//			}
//		}
		return new FeedbackRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), voList);
	}

	@Override
	public StatisticsRes statistics(int quizId) {
		if (quizId <= 0) {
			return new StatisticsRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		// 1. 取得問題和作答的資料
		List<QuestionAnswerDto> dtoList = fillinDao.selectQuestionAnswerList(quizId);
		// 2. 建立 Map 蒐集 ""所有題目"" 相同題號的所有作答: 作答也是選項，只是 List 中的字串會重複
		// Map<問題編號, 所有作答>
		Map<Integer, List<String>> quIdAnswerMap = new HashMap<>();
		for (QuestionAnswerDto dto : dtoList) {
			// 將 dto 中的 answerStr 轉換成 List<String>
			try {
				List<String> answerList = new ArrayList<>();
				if (dto.getAnswerStr() != null && !dto.getAnswerStr().isEmpty()) {
					answerList = mapper.readValue(dto.getAnswerStr(), new TypeReference<>() {
					});
				}
				if (quIdAnswerMap.containsKey(dto.getQuestionId())) {
					// 若 quIdAnswerMap 中已存在 key，則把 key 對應的 value 取出後，與新的值相加
					List<String> oldList = quIdAnswerMap.get(dto.getQuestionId());
					// 把 answerList 加入到原本的 List 中
					oldList.addAll(answerList);
					// 再把新的 List 放回到 quIdAnswerMap 中
					quIdAnswerMap.put(dto.getQuestionId(), oldList);
				} else {
					// quIdAnswerMap 中不存在 key，直接把 key-value 放到 quIdAnswerMap 中
					quIdAnswerMap.put(dto.getQuestionId(), answerList);
				}
			} catch (Exception e) {
				// 不需要 throw 因為沒有使用 @Transactional，就只有取資料而已，所以可以 return 自定義
				return new StatisticsRes(ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getCode(), //
						ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getMessage());
			}
		}
		// 3.取得每題問題
		List<Question> questionList = questionDao.getQuestionsByQuizId(quizId);
		// 建立題號和選項次數Vo陣列的 map: Map<題號, 選項次數VoList>
		Map<Integer, List<OptionCountVo>> quIdOptionVoListMap = new HashMap<>();
		for (Question question : questionList) {
			try {
				// 跳過選項題時的處理：簡答題的 options 為空
				if (!question.getType().equalsIgnoreCase(QuestionType.TEXT.getType())) {
					// 把選項字串轉換成 List
					List<String> optionList = mapper.readValue(question.getOptions(), new TypeReference<>() {
					});
					List<OptionCountVo> voList = new ArrayList<>();
					// 把選項 optionList 中的每個選項轉成 OptionCountVo
					for (String op : optionList) {
						// 每個 op 就是一個選項
						OptionCountVo vo = new OptionCountVo(op, 0);
						voList.add(vo);
					}
					quIdOptionVoListMap.put(question.getQuestionId(), voList);
				} else {
					// 簡答題的 OptionCountVo 設為空
					quIdOptionVoListMap.put(question.getQuestionId(), new ArrayList<>());
				}
			} catch (Exception e) {
				return new StatisticsRes(ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getCode(), //
						ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getMessage());
			}
		}
		// 4. 計算每題每個選項的次數
		for (Entry<Integer, List<OptionCountVo>> map : quIdOptionVoListMap.entrySet()) {
			int questionId = map.getKey();
			List<OptionCountVo> voList = map.getValue();
			// 1. 從 quIdAnswerMap 中，取得相同 key 對應的 value
			List<String> answerList = quIdAnswerMap.get(questionId);
			if (answerList == null)
				continue; // 若沒有作答，跳過
			for (OptionCountVo vo : voList) {
				// 紀錄 answerList 的 size，因為每次迴圈跑完 answerList 應該都會變更
				int size = answerList.size();
				String option = vo.getOption();
				// 移除所有 answerList 中的符合選項(option) --> 得到新的 answerList
				answerList.removeAll(List.of(option));
				// 紀錄新的 answerList 的 size
				int newSize = answerList.size();
				// 計算次數並把該值設定回 OptionCountVo 中
				int count = size - newSize;
				vo.setCount(count);
			}
		}
		// 5. 設定 res，遍歷所有題目，簡答題 OptionCountVo 為空
		List<StatisticsVo> statisticsVoList = new ArrayList<>();
		for (Question question : questionList) {
			List<OptionCountVo> voList = quIdOptionVoListMap.getOrDefault(question.getQuestionId(), new ArrayList<>());
			List<String> answers = quIdAnswerMap.getOrDefault(question.getQuestionId(), new ArrayList<>());

			StatisticsVo vo = new StatisticsVo(question.getQuestionId(), question.getQuestion(), question.getType(), //
					question.isRequired(), voList, answers);
			statisticsVoList.add(vo);
		}
		return new StatisticsRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), statisticsVoList);
	}
}
