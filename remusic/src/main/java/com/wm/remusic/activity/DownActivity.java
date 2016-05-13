package com.wm.remusic.activity;

import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.wm.remusic.R;
import com.wm.remusic.downmusic.DownloadDBEntity;
import com.wm.remusic.downmusic.DownloadManager;
import com.wm.remusic.downmusic.DownloadStatus;
import com.wm.remusic.downmusic.DownloadTask;
import com.wm.remusic.downmusic.DownloadTaskListener;
import com.wm.remusic.handler.HandlerUtil;
import com.wm.remusic.net.HttpUtil;
import com.wm.remusic.service.MediaService;
import com.wm.remusic.service.MusicPlayer;
import com.wm.remusic.uitl.IConstants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by wm on 2016/4/12.
 */
public class DownActivity extends AppCompatActivity{

    LinearLayout allStart,allStop,clear;
    ArrayList mList = new ArrayList();
    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;
    DownloadManager downloadManager;
    DownLoadAdapter adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_down);
//        ButterKnife.bind(this);

        allStart = (LinearLayout) findViewById(R.id.down_start_all);
        allStop = (LinearLayout) findViewById(R.id.down_pause_all);
        clear = (LinearLayout) findViewById(R.id.down_clear_all);
        setListener();

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DownLoadAdapter(null,null);
        recyclerView.setAdapter(adapter);
        HandlerUtil.getInstance(this).postDelayed(mUpdateProgress,100);
        reload();

    }

    public Runnable mUpdateProgress = new Runnable() {

        @Override
        public void run() {
            reload();

            HandlerUtil.getInstance(DownActivity.this).postDelayed(mUpdateProgress, 1000);

        }
    };

    private void reload(){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                downloadManager =  DownloadManager.getInstance(DownActivity.this);
                mList = (ArrayList) downloadManager.loadAllDownloadTaskFromDB();

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid){
                adapter.update(mList,downloadManager.getCurrentTaskList());
            }
        }.execute();
    }

    private void setListener(){


        allStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<DownloadTask> taskslist = (ArrayList) downloadManager.loadAllDownloadTaskFromDB();
                Iterator iterator = taskslist.iterator();
                while (iterator.hasNext()){
                    DownloadTask task = (DownloadTask) iterator.next();
                    downloadManager.resume(task.getId());
                }

            }
        });
        allStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<DownloadTask> taskslist = (ArrayList) downloadManager.loadAllDownloadTaskFromDB();
                Iterator iterator = taskslist.iterator();
                while (iterator.hasNext()){
                    DownloadTask task = (DownloadTask) iterator.next();
                    downloadManager.pause(task);
                }

            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<DownloadTask> taskslist = (ArrayList) downloadManager.loadAllDownloadTaskFromDB();
                Iterator iterator = taskslist.iterator();
                while (iterator.hasNext()) {
                    DownloadTask task = (DownloadTask) iterator.next();
                    downloadManager.cancel(task);
                }
            }
        });
    }



    class DownLoadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList mList;
        boolean showPro = false;
        private Map<String,DownloadTask> currentTaskList;

        public DownLoadAdapter(ArrayList list,Map<String,DownloadTask> currentTaskList){
            mList = list;
            this.currentTaskList = currentTaskList;
        }

        public void update(ArrayList list,Map<String,DownloadTask> currentTaskList){
            mList = list;
            this.currentTaskList = currentTaskList;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_down_item,parent,false));
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

             final DownloadTask task = (DownloadTask) mList.get(position);
             ((ItemViewHolder) holder).title.setText(task.getFileName());
             final boolean isCurrent = currentTaskList.containsKey(task.getId());


                if(task.getPercent() == 100){
                    ((ItemViewHolder) holder).artist.setText(task.getFileName());
                    ((ItemViewHolder) holder).count.setText((float) (Math.round((float) task.getCompletedSize() / (1024 * 1024) * 10)) / 10 + "M");
                    ((ItemViewHolder) holder).progressBar.setVisibility(View.GONE);
                    ((ItemViewHolder) holder).downloaded.setVisibility(View.VISIBLE);

                }else {
                    ((ItemViewHolder)holder).count.setText((float) (Math.round((float) task.getCompletedSize() / (1024 * 1024) * 10)) / 10 + "M/" +
                            (float) (Math.round((float) task.getTotalSize() / (1024 * 1024) * 10)) / 10 + "M");
                    ;
                    if(isCurrent){
                        ((ItemViewHolder) holder).progressBar.setVisibility(View.VISIBLE );
                        ((ItemViewHolder) holder).progressBar.setProgress((int) task.getPercent());

                    }else {
                        ((ItemViewHolder) holder).progressBar.removeCallbacks(mUpdateProgress);
                        ((ItemViewHolder) holder).progressBar.setVisibility(View.GONE );
                        ((ItemViewHolder) holder).count.setText("已经暂停，点击继续下载");
                    }


                    ((ItemViewHolder) holder).itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(!isCurrent){
                                downloadManager.resume(task.getId());
                                return;
                            }
                            if (task.getDownloadStatus() != DownloadStatus.DOWNLOAD_STATUS_COMPLETED) {

                                if (task.getDownloadStatus() == DownloadStatus.DOWNLOAD_STATUS_DOWNLOADING ) {
                                    downloadManager.pause(downloadManager.getTaskById(task.getId()));

                                } else {
                                    downloadManager.resume(task.getId());
                                }
                            }

                        }
                    });
                }

                ((ItemViewHolder) holder).clear.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(DownActivity.this).setTitle("要清除下载吗")
                                .setPositiveButton(DownActivity.this.getString(R.string.sure), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        downloadManager.cancel(task.getId());
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(DownActivity.this.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    }
                });



        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder{

            SimpleDraweeView draweeView;
            ImageView downloaded ,clear;
            TextView title,count,artist;
            ProgressBar progressBar;

            public ItemViewHolder(View itemView) {
                super(itemView);
                draweeView = (SimpleDraweeView) itemView.findViewById(R.id.down_img);
                title = (TextView) itemView.findViewById(R.id.down_top_text);
                count = (TextView) itemView.findViewById(R.id.down_count);
                clear = (ImageView) itemView.findViewById(R.id.down_single_clear);
                artist = (TextView) itemView.findViewById(R.id.down_artist);
                downloaded = (ImageView) itemView.findViewById(R.id.downloaded);
                progressBar = (ProgressBar) itemView.findViewById(R.id.down_progress);
                progressBar.setMax(100);

                clear.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

            }
        }


    }
}