package com.example.listviewtest;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class PullToReshListView extends ListView implements OnScrollListener{

	private static final int STATE_PULL_TO_REFRESH = 1;//下拉刷新
	private static final int STATE_RELEASE_TO_REFRESH = 2;//释放刷新
	private static final int STATE_REFRESHING = 3;//正在刷新
	private int mCurrentState = STATE_PULL_TO_REFRESH;//当前状态
	
	public static final int MODE_PULL_TO_REFRESH = 4;//下拉刷新
	public static final int MODE_LOAD_MORE = 5;//上拉加载更多
	public static final int MODE_BOTH = 6;//既可以下拉刷新又可以上拉加载更多
	private int modeRefresh = MODE_BOTH;//刷新模式，默认可以下拉刷新以及上拉加载更多
	
	private ImageView mIvArrow;//箭头图标
	private ProgressBar mPbLoading;//旋转进度条
	private TextView mTvTime;//刷新时间
	private TextView mTvTitle;//描述刷新状态的文字
	private int mHeadHeight;//头布局高度
	private View mHeadView;//头布局
	private RotateAnimation rotateUp;//向上旋转动画
	private RotateAnimation rotateDown;//向下旋转动画
	private int startY = -1;//滑动开始时的Y坐标
	
	private OnRefreshListener onRefreshListener;//刷新状态监听器
	
	private boolean isLoadMoreing = false;//是否正在加载更多
	private boolean isRefreshing = false;//是否正在刷新
	private View mFootView;//底部布局
	private int mFootHeight;//底部布局高度

	/**
	 * 设置刷新模式
	 * @param mode
	 * 		MODE_PULL_TO_REFRESHb(下拉刷新),MODE_LOAD_MORE(上拉加载更多)
	 * 		MODE_BOTH(既可以下拉刷新又可以上拉加载更多)
	 */
	public void setMode(int mode){
		modeRefresh = mode;
	}
	
	public PullToReshListView(Context context) {
		this(context, null);
	}

	public PullToReshListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PullToReshListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		//初始化头布局
		if(modeRefresh == MODE_BOTH || modeRefresh == MODE_PULL_TO_REFRESH){
			initHead();
		}
		
		//初始化底布局
		if(modeRefresh == MODE_BOTH || modeRefresh == MODE_LOAD_MORE){
			initFoot();
		}
	}
	
	private void initHead() {
		//生成头布局
		mHeadView = View.inflate(getContext(), R.layout.pull_to_refresh_head, null);
		addHeaderView(mHeadView);
		
		//需要根据滑动状态更新的文字和图标
		mIvArrow = (ImageView) mHeadView.findViewById(R.id.iv_arrow);
		mPbLoading = (ProgressBar) mHeadView.findViewById(R.id.pb_loading);
		mTvTime = (TextView) mHeadView.findViewById(R.id.tv_time);
		mTvTitle = (TextView) mHeadView.findViewById(R.id.tv_title);
		
		//平时需要隐藏头布局
		mHeadView.measure(0, 0);
		mHeadHeight = mHeadView.getMeasuredHeight();
		mHeadView.setPadding(0, -mHeadHeight, 0, 0);
		
		//初始化图标动画
		initAnimation();
		//设置更新时间
		setRefreshTime();
	}

	private void initFoot() {
		//生成底布局
		mFootView = View.inflate(getContext(), R.layout.pull_to_refresh_footer, null);
		addFooterView(mFootView);
		
		//隐藏底布局
		mFootView.measure(0, 0);
		mFootHeight = mFootView.getMeasuredHeight();
		mFootView.setPadding(0, -mFootHeight, 0, 0);
		
		//设置滑动监听
		setOnScrollListener(this);
	}

	@SuppressLint("SimpleDateFormat") 
	private void setRefreshTime() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = format.format(new Date());
		mTvTime.setText(time);
	}

	@SuppressLint("ClickableViewAccessibility") 
	private void initAnimation() {
		//向上旋转180度的动画
		rotateUp = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		rotateUp.setDuration(200);
		rotateUp.setFillAfter(true);
		
		//向下旋转180度的动画
		rotateDown = new RotateAnimation(-180, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		rotateDown.setDuration(200);
		rotateDown.setFillAfter(true);
	}
	
	//重写触摸事件，更新头布局和底布局
	@SuppressLint("ClickableViewAccessibility") @Override
	public boolean onTouchEvent(MotionEvent ev) {
		if(modeRefresh == MODE_BOTH || modeRefresh == MODE_PULL_TO_REFRESH){
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				//获取开始的y坐标
				startY = (int) ev.getY();
				break;

			case MotionEvent.ACTION_MOVE:
				//如果当前正在刷新，则不需要处理
				if(mCurrentState == STATE_REFRESHING){
					break;
				}
				
				//避免开始没有获取到坐标
				if(startY == -1){
					startY = (int) ev.getY();
				}
				
				//获取移动到的坐标
				int endY = (int) ev.getY();
				//获取触摸的位移
				int dy = endY - startY;
				//获取可显示的第一个条目
				int firstVisiblePosition = getFirstVisiblePosition();
				//当设置了刷新监听时，更新刷新布局
				if(onRefreshListener!=null){
					if(dy > 0 && firstVisiblePosition == 0){
						int padding = dy - mHeadHeight;
						mHeadView.setPadding(0, padding, 0, 0);
						if(padding > 0 && mCurrentState != STATE_RELEASE_TO_REFRESH){
							mCurrentState = STATE_RELEASE_TO_REFRESH;
							refreshState();
						}else if(padding < 0 && mCurrentState != STATE_PULL_TO_REFRESH){
							mCurrentState = STATE_PULL_TO_REFRESH;
							refreshState();
						}
					return true;
					}
				}
				break;
				
			case MotionEvent.ACTION_UP:
				startY = -1;
				//释放时，当状态为下拉刷新时，则不刷新，并隐藏布局
				if(mCurrentState == STATE_PULL_TO_REFRESH){
					mHeadView.setPadding(0, -mHeadHeight, 0, 0);
				}else if(mCurrentState == STATE_RELEASE_TO_REFRESH ){
					//释放时，当状态为释放刷新时，则显示布局，并刷新
					mHeadView.setPadding(0, 0, 0, 0);
					mCurrentState = STATE_REFRESHING;
					isRefreshing = true;
					if(onRefreshListener!=null) onRefreshListener.onRefresh();
					refreshState();
				}
				break;
			}
		}
		return super.onTouchEvent(ev);
	}

	private void refreshState() {
		switch (mCurrentState) {
		case STATE_PULL_TO_REFRESH:
			mTvTitle.setText("下拉刷新");
			mIvArrow.setVisibility(View.VISIBLE);
			mPbLoading.setVisibility(View.INVISIBLE);
			mIvArrow.startAnimation(rotateDown);
			break;
			
		case STATE_RELEASE_TO_REFRESH:
			mTvTitle.setText("释放刷新");
			mIvArrow.setVisibility(View.VISIBLE);
			mPbLoading.setVisibility(View.INVISIBLE);
			mIvArrow.startAnimation(rotateUp);
			break;
			
		case STATE_REFRESHING:
			mTvTitle.setText("正在刷新...");
			mIvArrow.clearAnimation();
			mIvArrow.setVisibility(View.INVISIBLE);
			mPbLoading.setVisibility(View.VISIBLE);
			break;
		}		
	}
	
	//监听刷新的接口
	public interface OnRefreshListener{
		/**
		 * 下拉刷新加载数据的方法
		 */
		public void onRefresh();
		/**
		 * 上拉加载更多加载数据的方法
		 */
		public void onLoadMore();
	}
	
	/**
	 * 设置刷新监听接口的方法
	 * @param onRefreshListener
	 * 		刷新监听接口
	 */
	public void setOnRefreshListener(OnRefreshListener onRefreshListener){
		this.onRefreshListener = onRefreshListener;
	}
	
	/**
	 * 加载数据完成后需要调用该方法
	 */
	public void onRefreshComplete(){
		if(isLoadMoreing){
			//如果是下拉加载更多，需要隐藏底布局
			mFootView.setPadding(0, -mFootHeight, 0, 0);
			isLoadMoreing = false;
		} 
		if(isRefreshing){
			//如果是下拉刷新，需要隐藏头布局
			mCurrentState = STATE_PULL_TO_REFRESH;
			mHeadView.setPadding(0, -mHeadHeight, 0, 0);
			mTvTitle.setText("下拉刷新");
			mIvArrow.setVisibility(View.VISIBLE);
			mPbLoading.setVisibility(View.INVISIBLE);
			mIvArrow.startAnimation(rotateDown);
			setRefreshTime();
			isRefreshing = false;
		}
	}

	//设置滚动监听
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if(scrollState == SCROLL_STATE_IDLE){
			//当滚动状态为空闲状态时，获取最后一条条目
			int lastVisiblePosition = getLastVisiblePosition();
			if(onRefreshListener!=null){
				//如果滚动了底部，则调用加载数据的方法
				if(lastVisiblePosition == getCount()-1 && !isLoadMoreing){
					isLoadMoreing = true;
					mFootView.setPadding(0, 0, 0, 0);
					if(onRefreshListener!=null) onRefreshListener.onLoadMore();
					setSelection(getCount()-1);
				}
			}
		}
	}
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,int visibleItemCount, int totalItemCount) {}

}
