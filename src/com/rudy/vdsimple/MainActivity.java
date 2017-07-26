package com.rudy.vdsimple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends Activity implements OnClickListener, OnItemClickListener {
	static class VideoTrack {
		String source = "";
		String videoId = "";
		String type = "";
		
		String date = "";
		String channelId = "";
		String channelTitle = "";
		
		String title = "";
		String description = "";
		
		String duration = "";
		String live = "";
		
		String downloadUrl = "";
	}
	
	ImageView mBtnSearch;
	ImageView mBtnCancel;
	Button mBtnEngine;
	
	boolean m_isEmpty = false;
	boolean m_isCanLoadMore = true;
	
	EditText mETSearch;
	TextView mTVTitle;
	ListView mLVVideos;
	
	ProgressDialog mLoadingDialog = null;
	
	ArrayList<VideoTrack> mVideos = new ArrayList<VideoTrack>();
	
	SearchEngine mEngine;
	VideoAdapter mAdapter;
	boolean m_isS1Engine = true; // S1 = ccMixter, S2= Youtube
	
	int PAGE_SIZE = 20;
	int mPageNumber = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mBtnSearch = (ImageView)findViewById(R.id.btn_search);
		mBtnCancel = (ImageView)findViewById(R.id.btn_cancel);
		mBtnEngine = (Button)findViewById(R.id.btn_engine);
		mETSearch = (EditText)findViewById(R.id.txt_search);
		mLVVideos = (ListView)findViewById(R.id.lv_videos);
		mTVTitle = (TextView)findViewById(R.id.txt_search_title);
		
		mBtnSearch.setOnClickListener(this);
		mBtnCancel.setOnClickListener(this);
		mBtnEngine.setOnClickListener(this);
		mLVVideos.setOnItemClickListener(this);
		
		mETSearch.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					if (mETSearch.getText().toString().length() == 0){
                			return true;
                		}
                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mETSearch.getWindowToken(), 0);
                    findViewById(R.id.header_lay2).setVisibility(View.GONE);
                    findViewById(R.id.header_lay1).setVisibility(View.VISIBLE);
                    
                    String keyword = mETSearch.getText().toString(); 
                    search(keyword);
                    mTVTitle.setText("Result for " + keyword);
                    mETSearch.setText("");
                    return true;
				}
				return false;
			}
		});
		
		mETSearch.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (mETSearch.length() != 0){
					mBtnCancel.setAlpha(255);
            			m_isEmpty = false;
				}
            		else if (m_isEmpty == false)
            			mBtnCancel.setAlpha(70);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
	}

	@Override
	public void onClick(View v) {
		if(v == mBtnSearch) {
			findViewById(R.id.header_lay1).setVisibility(View.GONE);
            findViewById(R.id.header_lay2).setVisibility(View.VISIBLE);
            mBtnCancel.setAlpha(70);
            
            mETSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mETSearch, InputMethodManager.SHOW_IMPLICIT);
		}
		else if(v == mBtnCancel) {
			if (mETSearch.length() != 0){
	        		m_isEmpty = true;
	        		mETSearch.setText("");
	        		return;
	        	}
        	
        		findViewById(R.id.header_lay2).setVisibility(View.GONE);
        		findViewById(R.id.header_lay1).setVisibility(View.VISIBLE);
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mETSearch.getWindowToken(), 0);
		} else if (v == mBtnEngine) {
			m_isS1Engine = !m_isS1Engine;
			mBtnEngine.setText(m_isS1Engine ? "S1" : "S2");
			
			if (mKeyword.length() > 0) {
				if (findViewById(R.id.header_lay1).getVisibility() == View.GONE) {
					if (mETSearch.length() != 0){
			        		m_isEmpty = true;
			        		mETSearch.setText("");
			        	}
	        	
		        		findViewById(R.id.header_lay2).setVisibility(View.GONE);
		        		findViewById(R.id.header_lay1).setVisibility(View.VISIBLE);
		            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mETSearch.getWindowToken(), 0);
				}
				search(mKeyword);
				Toast.makeText(this, "Music search source is changed", Toast.LENGTH_SHORT).show();
			}
    		}
	}
	
	String mKeyword = "";
	public void search(String keyword) {
		mVideos = new ArrayList<VideoTrack>();
		mAdapter = null;
		mKeyword = keyword;
		mPageNumber = 0;
		m_isCanLoadMore = true;
		
		if (m_isS1Engine) {
			(new SearchCcmixter()).execute(mKeyword, mPageNumber, PAGE_SIZE);
		} else {
			(new SearchYouTube()).execute(mKeyword, mPageNumber, PAGE_SIZE);
		}
	}
	
	public void loadMore() {
		if(mLoadingDialog != null) {
			return;
		}
		mPageNumber ++;
		
		if (m_isS1Engine) {
			(new SearchCcmixter()).execute(mKeyword, mPageNumber, PAGE_SIZE);
		} else {
			(new SearchYouTube()).execute(mKeyword, mPageNumber, PAGE_SIZE);
		} 
	}
	
	public void onSearched(ArrayList<VideoTrack> videos) {
		if (videos.size() == 0) {
			m_isCanLoadMore = false;
			return;
		}
		mVideos.addAll(videos);
		
		if (mAdapter == null) {
			mAdapter = new VideoAdapter(this, mVideos);
			mLVVideos.setAdapter(mAdapter);
		} else {
			mAdapter.notifyDataSetChanged();
		}
	}
	
	public void showVideo(final VideoTrack video) {
		final Dialog dialog = new Dialog(this);
    		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.download_dialog);
	    
		// set the custom dialog components - text, image and button
		TextView text = (TextView) dialog.findViewById(R.id.tv_title);
		text.setText(video.title);

		WebView displayYoutubeVideo = (WebView)dialog.findViewById(R.id.wv_video);
		displayYoutubeVideo.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        WebSettings webSettings = displayYoutubeVideo.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        if (video.source.equals("YouTube")) {
        		String emBedUrl = "<html><style type='text/css'>.video-container { position: relative; padding-bottom: 56.25%; padding-top: 30px; height: 0; overflow: hidden; } " +
        			   ".video-container iframe, .video-container object, .video-container embed { position: absolute; top: 0; left: 0; width: 100%; height: 100%;}" + 
        			   "</style><body><div class='video-container'><iframe class=\"youtube-player\" type=\"text/html\" width=\"400\" height=\"300\" src=\"http://www.youtube.com/embed/" + video.videoId + "\" frameborder=\"0\"></div></body></html>";
    	        if(!emBedUrl.isEmpty()) {
    	        		displayYoutubeVideo.loadData(emBedUrl, "text/html", "utf-8");
    	        }
    	        else {
    	        		displayYoutubeVideo.loadUrl("https://m.youtube.com/watch?v=" + video.videoId );
    	        }        	
        } else {        		
        		displayYoutubeVideo.loadUrl(video.downloadUrl);
        }
		
		TextView btnDownloadMp3 = (TextView) dialog.findViewById(R.id.tv_download_mp3);
        btnDownloadMp3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				downloadFile(video.downloadUrl);
			}
		});
        
        TextView btnDownloadVideo = (TextView) dialog.findViewById(R.id.tv_download_video);
        
        if (m_isS1Engine) {
        		btnDownloadVideo.setVisibility(View.GONE);
        } else {
	        	btnDownloadVideo.setOnClickListener(new OnClickListener() {
	    			@Override
	    			public void onClick(View v) {
	    				downloadFile(video.downloadUrl);
	    			}
	    		});
        }
        
        TextView btnClose = (TextView) dialog.findViewById(R.id.tv_cancel);
        btnClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
	    lp.copyFrom(dialog.getWindow().getAttributes());
	    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
	    
		dialog.show();
		dialog.getWindow().setAttributes(lp);
	}
	
	public void downloadFile(String url) {
		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Request request = new Request(
                Uri.parse(url));
        dm.enqueue(request);
	}
	
	public class SearchEngine extends AsyncTask<Object, Void, Boolean>
    {
		ArrayList<VideoTrack> listVideos;
		
        public String GetUrl(String keyword, int page, int limit) {
        		return null;
        }
        
        public void processResult(String content) {
        }
        
        public String getStringFromURL(String strURL) {
        		StringBuffer chaine = new StringBuffer("");
            try{
                URL url = new URL(strURL);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();

                InputStream inputStream = connection.getInputStream();

                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    chaine.append(line);
                }
            }
            catch (IOException e) {
                // Writing exception to log
                e.printStackTrace();
                return null;
            }
            return chaine.toString();
        }
        
        protected Boolean doInBackground(Object... as)
        {   	
	        	try {
	        		listVideos = new ArrayList<VideoTrack>();
	        		
	        		String search_text = URLEncoder.encode(as[0].toString(), "UTF-8");
	        		int offset = (Integer)as[1];
	        		int limit = (Integer)as[2];
	        		
	        		String content = getStringFromURL(GetUrl(search_text, offset, limit));
	        		if (content == null || content.length() == 0) {
	        			return false;
	        		}
	        		
	        		processResult(content);
		    }
	        catch(Exception e) {
	        }
	        	            
            return true;
        }

        protected void onPostExecute(Boolean result)
        {
	        	super.onPostExecute(result);
	        	if(mLoadingDialog != null) {
	        		mLoadingDialog.dismiss();
	        		mLoadingDialog = null;
	        	}
        		onSearched(listVideos);
        }

        protected void onPreExecute()
        {
            super.onPreExecute();
            mLoadingDialog = ProgressDialog.show(MainActivity.this, "",  "Searching. Please wait...", true);
            mLoadingDialog.setCancelable(false);
        }
    }
		
	String nextPageToken = "";
	
	public class SearchYouTube extends SearchEngine{
	    private String YOUTUBE_BASE_URL = "https://www.googleapis.com/youtube/v3/search?part=snippet&key=AIzaSyB-vE_PNo2_1o65I2etL3aITlKRYYhzeFs&q=";

	    @Override
	    public String GetUrl(String keyword, int page, int limit) {
	        if (page > 0)
	        		return YOUTUBE_BASE_URL + keyword + "&maxResults=" + limit + "&pageToken=" + nextPageToken;
	        
	        return YOUTUBE_BASE_URL + keyword + "&maxResults=" + limit;
	    }
	    
	    public void setISO8601Duration(String ptdur, int i) {
			String result = ptdur.replace("PT","").replace("H",":").replace("M",":").replace("S","");
	        String arr[] = result.split(":");
	        if(arr.length > 2) {
	        		listVideos.get(i).duration = String.format("%d:%02d:%02d", Integer.parseInt(arr[0]), Integer.parseInt(arr[1]), Integer.parseInt(arr[2]));
	        }
	        else if(arr.length == 2) {
	        		listVideos.get(i).duration = String.format("%02d:%02d", Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
	        }
	        else if(arr.length == 1) {
	        		listVideos.get(i).duration = String.format("00:%02d", Integer.parseInt(arr[0]));
	        }
		}
	    
	    @Override
	    public void processResult(String content) {
	        String videoIds = "";
	        
            try {
	            	JSONObject jObject = new JSONObject(content);
	            	if(jObject.has("nextPageToken")) {
	            		nextPageToken = jObject.getString("nextPageToken");
	            	}
            	
	            	if(jObject.has("items")) {
	            		JSONArray array = jObject.getJSONArray("items");
	            		for(int i = 0; i < array.length(); i++) {
	            			try {
	            				VideoTrack video = new VideoTrack();
	            				video.source = "YouTube";
	            				
	            				JSONObject item = array.getJSONObject(i);
	            				
	            				JSONObject id = item.getJSONObject("id");
	            				video.type = id.getString("kind");
	            				if(id.has("videoId")) {
	            					video.videoId = id.getString("videoId");
	            				}
	            				else if(id.has("channelId")) {
	            					//video.videoId = id.getString("channelId");
	            					//let's don't show channels
	            					continue;
	            				}
	            				
	            				JSONObject snippet = item.getJSONObject("snippet");
	            				video.date = snippet.getString("publishedAt");
	            				if(snippet.has("channelId")) {
	            					video.channelId = snippet.getString("channelId");
	            				}
	            				video.title = snippet.getString("title");
	            				video.description = snippet.getString("description");
	            				video.channelTitle = snippet.getString("channelTitle");
	            				video.live = snippet.getString("liveBroadcastContent");
	            				
	            				video.duration="??:??";
	            				video.downloadUrl = "http://api.soundcloud.com/tracks/159723640/stream?client_id=40ccfee680a844780a41fbe23ea89934";
	            				
	            				listVideos.add(video);
	            				
	            				videoIds = videoIds + (videoIds.isEmpty() ? "" : ",") + video.videoId;
	            				
	            			} catch(Exception e) {
	            				e.printStackTrace();
	            			}
	            		}
	            	}
	            	
	            	//get durations
	            String strURL = "https://www.googleapis.com/youtube/v3/videos?id=" + videoIds + "&part=contentDetails&key=AIzaSyB-vE_PNo2_1o65I2etL3aITlKRYYhzeFs";
	            String contentDetails = getStringFromURL(strURL);
	            
	            jObject = new JSONObject(contentDetails);
	            if(jObject.has("items")) {
		        		JSONArray array = jObject.getJSONArray("items");
		        		for(int i = 0; i < array.length(); i++) {
		        			if (!listVideos.get(i).live.equals("none")) {
		        				listVideos.get(i).duration = listVideos.get(i).live;
		        				continue;
		        			}
		        			
		        			JSONObject item = array.getJSONObject(i);
		    				
		        			try {
		        				JSONObject detail = item.getJSONObject("contentDetails");
		        				setISO8601Duration(detail.getString("duration"), i);
		        			}
		        			catch(Exception e) {
		        				e.printStackTrace();
		        			}
		        		}
	            }
		        	
	        } catch(Exception e) {

	        }
	    }
	    
	}
	
	public class SearchCcmixter extends SearchEngine {
	    private String CCMIXTER_BASE_URL = "http://ccmixter.org/api/query";

	    @Override
	    public String GetUrl(String keyword, int page, int limit ) {
	    		return CCMIXTER_BASE_URL + "&search=" + keyword + "&limit=" + limit + "&offset=" + (page * limit) + "&f=js&reqtags=remix&search_type=any";
	    }

	    @Override
	    public void processResult(String content) {
	        try {
	            JSONArray results = new JSONArray(content);
	            for (int i = 0; i < results.length(); i++) {
	                try {
	                    JSONObject track = results.getJSONObject(i);

	                    VideoTrack sTrack = new VideoTrack();
	                    sTrack.source = "Ccmixter";
	                    sTrack.videoId = track.getString("upload_id"); 
	                    
	                    sTrack.channelTitle = track.getString("user_name");
	                    sTrack.title = track.getString("upload_name");
	                    
	                    JSONArray files = track.getJSONArray("files");
		    				JSONObject urlObject = null; //urlList.getJSONObject(0);
		    				for(int j = 0; j< files.length(); j++) {
		    					urlObject = files.getJSONObject(j);
		    					if(urlObject.getString("file_nicname").equals("mp3")) {
		    						break;
		    					}
		    				}
		    				if(urlObject != null) {
		    					sTrack.downloadUrl = urlObject.getString("download_url");
		    					sTrack.duration = urlObject.getJSONObject("file_format_info").getString("ps");
		    				}
	                    listVideos.add(sTrack);
	                }
	                catch(Exception e) {
	                    continue;
	                }
	            }
	        } catch(Exception e) {
	        }
	    }

	}
	
	public class VideoAdapter extends BaseAdapter{
		private List<VideoTrack> array;
		private Context mContext;
		int selectedPosition = 0;

		public VideoAdapter(Context context, List<VideoTrack> str) {
			// TODO Auto-generated constructor stub
			mContext = context;
			array = str;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return array.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return array.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		public VideoTrack GetDownload(){
			return array.get(selectedPosition);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.download_list_item, null);
            }
            TextView tvTitle = (TextView)v.findViewById(R.id.tv_title);
            TextView tvAuthor = (TextView)v.findViewById(R.id.tv_uploader);
            TextView tvDuaration = (TextView)v.findViewById(R.id.tv_length);
            
            VideoTrack info = array.get(position);
            tvTitle.setText(info.title);
            tvAuthor.setText(info.channelTitle);
            tvDuaration.setText(info.duration);
            
            if(m_isCanLoadMore && array.size() > 5 && position > array.size() - 2) {
            		loadMore();
            }
            return v;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		if(position < mVideos.size()) {
			VideoTrack video = mVideos.get(position);
			showVideo(video);
		}
	}
}
