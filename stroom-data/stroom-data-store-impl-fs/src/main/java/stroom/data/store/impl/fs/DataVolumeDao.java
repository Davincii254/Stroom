package stroom.data.store.impl.fs;

import stroom.data.store.impl.fs.shared.FsVolume;
import stroom.util.shared.ResultPage;

import java.util.List;

public interface DataVolumeDao {

    ResultPage<DataVolume> find(FindDataVolumeCriteria criteria);

    DataVolume findDataVolume(long metaId);

    DataVolume createDataVolume(long metaId, FsVolume volume);

    int delete(List<Long> metaIdList);

    interface DataVolume {

        long getMetaId();

        String getVolumePath();
    }
}
