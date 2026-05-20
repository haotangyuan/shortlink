package dev.haotangyuan.shortlink.common.convention.exception;

import dev.haotangyuan.shortlink.common.convention.errorcode.BaseErrorCode;
import dev.haotangyuan.shortlink.common.convention.errorcode.IErrorCode;

/**
 * 远程服务调用异常
 *
 * @author: haotangyuan
 */
public class RemoteException extends AbstractException {

    public RemoteException(String message) {
        this(message, null, BaseErrorCode.REMOTE_ERROR);
    }

    public RemoteException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public RemoteException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }

    @Override
    public String toString() {
        return "RemoteException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}