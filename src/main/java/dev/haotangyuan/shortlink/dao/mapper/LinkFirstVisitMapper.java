package dev.haotangyuan.shortlink.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.shortlink.dao.entity.LinkFirstVisitDO;
import org.apache.ibatis.annotations.Insert;

/**
 * 首次访问判重表 Mapper
 * @author: haotangyuan
 */
public interface LinkFirstVisitMapper extends BaseMapper<LinkFirstVisitDO> {

    @Insert("INSERT IGNORE INTO t_link_first_visit (full_short_url, `user`) VALUES (#{fullShortUrl}, #{user})")
    int insertIgnore(LinkFirstVisitDO entity);
}
