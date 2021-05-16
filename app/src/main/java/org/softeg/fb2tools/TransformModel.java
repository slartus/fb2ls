package org.softeg.fb2tools;

import android.database.Observable;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.HashMap;

/*
 * Created by slinkin on 13.05.2015.
 */
public class TransformModel {
    private static final String TAG = "TransformModel";
    private final TransformObservable mObservable = new TransformObservable();
    private TransformTask mTransformTask;
    private HashMap<String, StringBuilder> mBooksWarnings = new HashMap<>();
    private boolean mIsWorking;

    public void transform(String[] paths) {
        if (mIsWorking) {
            return;
        }

        mObservable.notifyStarted();

        mIsWorking = true;
        mTransformTask = new TransformTask(paths);
        mTransformTask.execute();
    }

    public void stopTransform() {
        if (mIsWorking) {
            mTransformTask.cancel(true);
            mIsWorking = false;
        }
    }

    public HashMap<String, StringBuilder> getBooksWarnings(){
        return mBooksWarnings;
    }


    public void registerObserver(final Observer observer) {
        mObservable.registerObserver(observer);
        if (mIsWorking) {
            observer.onTransformStarted(this);
        }
    }

    public void unregisterObserver(final Observer observer) {
        mObservable.unregisterObserver(observer);
    }

    private class TransformTask extends AsyncTask<Void, Void, Boolean> {
        private String[] mPaths;

        public TransformTask(String[] paths) {

            mPaths = paths;
        }

        private Throwable ex;

        @Override
        protected Boolean doInBackground(final Void... params) {
            try {
                for (String path : mPaths) {
                    File file = new File(path);
                    if (!file.exists())
                        continue;
                    if (file.isDirectory()) {
                        transformDirRecursive(path);
                    } else {
                        transformFile(path);
                    }
                }
                return true;
            } catch (Throwable e) {
                ex = e;
                Log.i(TAG, "transform interrupted");
                return false;
            }
        }

        private void transformDirRecursive(String path) throws Exception {
            File dir = new File(path);
            for (String fileName : dir.list()) {
                if (".".equals(fileName) || "..".equals(fileName))
                    continue;
                String fullPAth = path + File.separator + fileName;
                File f = new File(fullPAth);
                if (f.isDirectory())
                    transformDirRecursive(fullPAth);
                else {
                    transformFile(fullPAth);
                }

            }
        }

        private void transformFile(String path) throws Exception {

            if (path.toLowerCase().endsWith(".fb2")) {
                Fb2Transformer transformer = new Fb2Transformer(path);
                transformer.parse();
                StringBuilder sb = transformer.getWarnings();
                if (sb.length() > 0)
                    mBooksWarnings.put(path.substring(path.lastIndexOf("/") + 1), transformer.getWarnings());
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mIsWorking = false;

            if (success) {
                mObservable.notifyCompleted();
            } else {
                mObservable.notifyFailed(ex);
            }
        }
    }


    public interface Observer {
        void onTransformStarted(TransformModel signInModel);

        void onTransformCompleted(TransformModel signInModel);

        void onTransformFailed(TransformModel signInModel, Throwable ex);
    }

    private class TransformObservable extends Observable<Observer> {
        public void notifyStarted() {
            for (final Observer observer : mObservers) {
                observer.onTransformStarted(TransformModel.this);
            }
        }

        public void notifyCompleted() {
            for (final Observer observer : mObservers) {
                observer.onTransformCompleted(TransformModel.this);
            }
        }

        public void notifyFailed(Throwable ex) {
            for (final Observer observer : mObservers) {
                observer.onTransformFailed(TransformModel.this, ex);
            }
        }

    }
}
