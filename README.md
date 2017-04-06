# PullToReshListView
带有下拉刷新和上拉加载更多的ListView，可以通过public void setMode(int mode)；方法设置其模式，mode可设置为如下值：MODE_PULL_TO_REFRESH(下拉刷新)，MODE_LOAD_MORE(上拉加载更多)，
MODE_BOTH(既可以下拉刷新又可以上拉加载更多)。

通过public void setOnRefreshListener(OnRefreshListener onRefreshListener)方法设置刷新监听器，完成加载数据的操作，并且在数据加载完成后调用public void onRefreshComplete()；结束刷新状态。
	
