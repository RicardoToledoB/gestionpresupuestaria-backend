package cl.dssm.presupuesto.exception;

public class MercadoPublicoRateLimitException extends RuntimeException {
    public MercadoPublicoRateLimitException(String message) {
        super(message);
    }

    public MercadoPublicoRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
