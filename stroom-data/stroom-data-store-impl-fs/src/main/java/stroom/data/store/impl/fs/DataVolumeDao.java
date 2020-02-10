package stroom.data.store.impl.fs;

import stroom.data.store.impl.fs.shared.FsVolume;

import stroom.util.shared.ResultList;

import java.util.List;

public interface DataVolumeDao {
    ResultList<DataVolume> find(FindDataVolumeCriteria criteria);

    DataVolume findDataVolume(long dataId);

    DataVolume createDataVolume(long dataId, FsVolume volume);

    int delete(List<Long> metaIdList);

    interface DataVolume {
        long getStreamId();

        String getVolumePath();
    }
}
