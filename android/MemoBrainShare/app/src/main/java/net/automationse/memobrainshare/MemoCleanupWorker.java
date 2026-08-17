package net.automationse.memobrainshare;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Hard retention cap for encrypted pending data, independent of network availability. */
public class MemoCleanupWorker extends Worker {
    public MemoCleanupWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String jobId = getInputData().getString(MemoSaveWorker.KEY_JOB_ID);
        if (jobId != null && !jobId.trim().isEmpty() && PendingJobStore.exists(getApplicationContext(), jobId)) {
            HistoryStore.updateStatus(getApplicationContext(), jobId, HistoryStore.EXPIRED);
            PendingJobStore.delete(getApplicationContext(), jobId);
        }
        PendingJobStore.cleanupExpired(getApplicationContext());
        return Result.success();
    }
}
