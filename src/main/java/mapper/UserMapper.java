package mapper;


import model.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author Jin.D
 * @date: 2018年3月15日 上午10:02:39
 */


public interface UserMapper {
    @Select("select * from user where id = #{id}")
    User getUserById(@Param("id") String id);

}
