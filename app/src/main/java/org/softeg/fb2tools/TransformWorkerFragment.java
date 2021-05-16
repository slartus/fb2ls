package org.softeg.fb2tools;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/*
 * Created by slinkin on 13.05.2015.
 */
public class TransformWorkerFragment  extends Fragment {
    private final TransformModel mTransformModel;

    public TransformWorkerFragment() {
        mTransformModel = new TransformModel();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public TransformModel getTransformModel() {
        return mTransformModel;
    }
}
