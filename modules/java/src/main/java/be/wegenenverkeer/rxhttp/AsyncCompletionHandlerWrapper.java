package be.wegenenverkeer.rxhttp;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.exceptions.OnErrorFailedException;
import rx.subjects.AsyncSubject;

import java.util.function.Function;

import static be.wegenenverkeer.rxhttp.CompleteResponseHandler.withCompleteResponse;
import static be.wegenenverkeer.rxhttp.ServerResponse.wrap;

/**
 * A {@link AsyncCompletionHandler} that pushes received items to a specified {@link AsyncSubject}
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 18/12/14.
 */
class AsyncCompletionHandlerWrapper<F> extends AsyncCompletionHandler<F> {

    final private static Logger logger = LoggerFactory.getLogger(AsyncCompletionHandlerWrapper.class);

    final private AsyncSubject<? super F> subject;
    final private Function<ServerResponse, F> handler;


    /**
     * Constructs a new instance with specified subject and response transform
     *
     * @param subject   the subject that receives the ServerResponse, after transformation
     * @param transform the transformation function
     */
    AsyncCompletionHandlerWrapper(AsyncSubject<? super F> subject, Function<ServerResponse, F> transform) {
        if (subject == null || transform == null) throw new IllegalArgumentException();
        this.subject = subject;
        this.handler = transform;
    }

    @Override
    public F onCompleted(Response response) throws Exception {
        try {
            withCompleteResponse(
                    response,
                    (r) -> {
                        F value = handler.apply(wrap(r));
                        if (value != null) subject.onNext(value);
                        subject.onCompleted();
                    },
                    (ex) -> subject.onError(ex),
                    (ex) -> subject.onError(ex)
            );
        } catch (Throwable t) {
            //TODO Should this logging not be done in the global onError handler? See Class RxJavaErrorHandler
            if (t instanceof OnErrorFailedException) {
                logger.error("onError handler failed: " + t.getMessage(), t);
            }
            subject.onError(t);
        }
        return null;
    }

    @Override
    public void onThrowable(Throwable t) {
        subject.onError(t);
    }


}
