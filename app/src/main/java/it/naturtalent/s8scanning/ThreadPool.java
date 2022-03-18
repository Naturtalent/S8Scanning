package it.naturtalent.s8scanning;

import com.techyourchance.threadposter.BackgroundThreadPoster;
import com.techyourchance.threadposter.UiThreadPoster;

public class ThreadPool
{
    /*
      IMPORTANT:
      Both BackgroundThreadPoster and UiThreadPoster should be global objects (single instance).
     */
    private final BackgroundThreadPoster mBackgroundThreadPoster = new BackgroundThreadPoster();
    private final UiThreadPoster mUiThreadPoster = new UiThreadPoster();

    private final DataFetcher mDataFetcher = new DataFetcher();

    private final ConnectionUseCase mConnectionUseCase =
            new ConnectionUseCase(mDataFetcher, mBackgroundThreadPoster, mUiThreadPoster);
    public ConnectionUseCase getConnectionUseCase() {return mConnectionUseCase; }

}
