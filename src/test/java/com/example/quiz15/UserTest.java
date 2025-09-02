package com.example.quiz15;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import com.example.quiz15.dao.UserDao;
import com.example.quiz15.entity.User;

/**
* @SpringBootTest: 有加上此註釋表示在執行測試方法之前，會先啟動整個專案，然後讓專案中
* 原本有被託管的物件建立起來，因此在測試方法時需要使用到被託管的物件時，可以正常被注入；
* 反之要求注入沒有被託管的物件，該物件就會是 null</br>
* @TestInstance(TestInstance.Lifecycle.PER_CLASS): 測試類別中有使用到 @BeforeAll 或是
* @AfterAll 時要加的註釋
*/
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
public class UserTest {

	@Autowired
	private UserDao userDao;

	private int res;

	// @BeforeEach: 在執行每個 @Test 方法之 ""前"" 會執行有加上此註釋的方法
	@BeforeEach
	public void addTestInfo() {
		// 測試新增資訊
		res = userDao.addInfo("A07", "0912345677", "a7@gmail.com", 29, "123");
	}

	// @AfterEach: 在執行每個 @Test 方法之 ""後"" 會執行有加上此註釋的方法
	@AfterEach
	public void deleteTestInfo() {
		userDao.deleteInfo("a7@gmail.com");
	}

	// @BeforeAll: 在整個測試執行中，只會執行一次，執行時間點是在所有測試方法執行之前
	@BeforeAll
	public void beforeAll() {
		System.out.println("Before All~~~~~");
	}

	// @AfterAll: 在整個測試執行中，只會執行一次，執行時間點是在所有測試方法結束之後
	@AfterAll
	public void afterAll() {
		System.out.println("After All~~~~~");
	}

	@BeforeEach
	public void beforeEach() {
		System.out.println("Before Each !!!!!!");
	}

	@AfterEach
	public void afterEach() {
		System.out.println("After Each !!!!!!");
	}

	@Test
	public void addInfoDaoTest() {
		try {
			// 測試新增資訊
//			int res = userDao.addInfo("A07", "0912345677", "a7@gmail.com", 29, "123");
			// 測試資料已在每個測試方法之前已新增
			// 確認 res 是否等於 1，後面的訊息表示前面的判斷式不成立時返回的訊息
			Assert.isTrue(res == 1, "addInfo failed!!");

			// 測試資料已在每個測試方法之後已刪除

			// 最後會將新增的測試資料刪除
//			userDao.deleteInfo("a7@gmail.com");
		} catch (Exception e) {

		}

	}

	@Test
	public void getCountByEmailDaoTest() {
		// 測試尚未新增資訊之前是否可以得到結果
//		int res = userDao.getCountByEmail("a7@gmail.com");
//		Assert.isTrue(res == 0, "getCountByEmail failed!!");

		// 測試新增資訊: 這邊不需要用一個變數將結果接回來，因為前面已經有測試過 addInfo 是OK的
//		addTestInfo();
//		userDao.addInfo("A07", "0912345677", "a7@gmail.com", 29, "123");

		res = userDao.getCountByEmail("a7@gmail.com");
		// 確認 res 是否等於 1，後面的訊息表示前面的判斷式不成立時返回的訊息
		Assert.isTrue(res == 1, "getCountByEmail failed!!");

		// 最後會將新增的測試資料刪除
//		userDao.deleteInfo("a7@gmail.com");
	}

	@Test
	public void getByEmailDaoTest() {
		// 測試新增資訊:
//		userDao.addInfo("A07", "0912345677", "a7@gmail.com", 29, "123");

		User res = userDao.getByEmail("a7@gmail.com");
		Assert.isTrue(res != null, "getByEmail failed!!");

		// 最後會將新增的測試資料刪除
//		userDao.deleteInfo("a7@gmail.com");
	}
}
