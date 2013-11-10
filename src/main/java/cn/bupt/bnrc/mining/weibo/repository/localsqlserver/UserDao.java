package cn.bupt.bnrc.mining.weibo.repository.localsqlserver;

import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface UserDao {

	public Map<String, Object> getUserInfoById(int userId);
}
