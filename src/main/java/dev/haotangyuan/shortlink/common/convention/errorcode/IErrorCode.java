package dev.haotangyuan.shortlink.common.convention.errorcode;

/**
 * 平台错误码
 * @author: haotangyuan
 */
public interface IErrorCode {

    /**
     * 错误码
     */
    String code();

    /**
     * 错误信息
     */
    String message();
}
