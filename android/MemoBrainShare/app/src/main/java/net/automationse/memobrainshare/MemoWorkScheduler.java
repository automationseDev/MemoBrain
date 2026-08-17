package net.automationse.memobrainshare;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class MemoWorkScheduler {
    private MemoWorkScheduler() {}

    public static void enqueue(Context context, String jobId, ExistingWorkPolicy policy) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data input = new Data.Builder().putString(MemoSaveWorker.KEY_JOB_ID, jobId).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MemoSaveWorker.class)
                .setInputData(input)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("memobrain-save")
                .build();

        WorkManager manager = WorkManager.getInstance(context.getApplicationContext());
        manager.enqueueUniqueWork("memobrain-save-" + jobId, policy, request);

        OneTimeWorkRequest cleanup = new OneTimeWorkRequest.Builder(MemoCleanupWorker.class)
                .setInputData(input)
                .setInitialDelay(PendingJobStore.MAX_RETENTION_MILLIS, TimeUnit.MILLISECONDS)
                .addTag("memobrain-cleanup")
                .build();
        manager.enqueueUniqueWork("memobrain-cleanup-" + jobId, ExistingWorkPolicy.KEEP, cleanup);
    }
}
