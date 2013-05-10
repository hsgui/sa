package cn.bupt.bnrc.mining.weibo.repository.localsqlserver;

import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public interface UserDao {

	public Map<String, Object> getUserInfoById(int userId);
}
