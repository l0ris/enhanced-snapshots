package enhancedsnapshots.dto.converter;

import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeAttachment;
import com.sungardas.enhancedsnapshots.dto.VolumeDto;
import com.sungardas.enhancedsnapshots.dto.converter.VolumeDtoConverter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;


public class VolumeDtoConverterTest {
    private static final String VOLUME_NAME = "First";
    private static final String VOLUME_ID = "vol-69dee6a09b";
    private static final int SIZE = 5;
    private static final String VOLUME_TYPE = "gp2";
    private static final String SNAPSHOT_ID = "snap-bf56423yds";
    private static final Date CREATE_TIME = new Date(1437477836902L);
    private static final String AVAILABILITY_ZONE = "us-east-1e";
    private static final String STATE = "in use";
    private static final String INSTANCE_ID = "i-d751bf7c7sd";
    private List<Tag> tags;

    @Before
    public void setUp() {
        tags = new ArrayList<>();
        tags.add(new Tag("Name", VOLUME_NAME));
    }

    @Test
    public void convertVolumeToVolumeDto() {
        // convert volume without attachments
        Volume volume = createVolume(VOLUME_ID, SNAPSHOT_ID, CREATE_TIME, AVAILABILITY_ZONE, SIZE, STATE, tags, null);
        VolumeDto volumeDto = VolumeDtoConverter.convert(volume);
        assertVolumeDtoFields(volumeDto, VOLUME_ID, SNAPSHOT_ID, CREATE_TIME, AVAILABILITY_ZONE, SIZE, STATE, tags, null);

        // convert attached volume
        volume = createVolume(VOLUME_ID, SNAPSHOT_ID, CREATE_TIME, AVAILABILITY_ZONE, SIZE, STATE, tags, INSTANCE_ID);
        volumeDto = VolumeDtoConverter.convert(volume);
        assertVolumeDtoFields(volumeDto, VOLUME_ID, SNAPSHOT_ID, CREATE_TIME, AVAILABILITY_ZONE, SIZE, STATE, tags, INSTANCE_ID);

        // add field to volume that is missing in VolumeDto object
        volume.setVolumeType(VOLUME_TYPE);
        Assert.assertTrue(volumeDto.equals(VolumeDtoConverter.convert(volume)));
    }

    @Test
    public void convertVolumeList() {
        List<Volume> volumeList = new ArrayList<>();
        //with tags
        volumeList.add(createVolume(VOLUME_ID, SNAPSHOT_ID, CREATE_TIME, AVAILABILITY_ZONE, SIZE, STATE, tags, INSTANCE_ID));

        // without tags
        volumeList.add(createVolume(VOLUME_ID + "_", SNAPSHOT_ID + "_", CREATE_TIME, AVAILABILITY_ZONE + "_",
                SIZE + 1, STATE + "_", new ArrayList<Tag>(), INSTANCE_ID + "_"));

        Set<VolumeDto> volumeDtoList = VolumeDtoConverter.convert(volumeList);
        Assert.assertTrue(volumeDtoList.size() == 2);
        assertVolumeDtoFields((VolumeDto) volumeDtoList.toArray()[0], VOLUME_ID, SNAPSHOT_ID, CREATE_TIME, AVAILABILITY_ZONE, SIZE, STATE, tags, INSTANCE_ID);
        assertVolumeDtoFields((VolumeDto) volumeDtoList.toArray()[1], VOLUME_ID + "_", SNAPSHOT_ID + "_", CREATE_TIME, AVAILABILITY_ZONE + "_",
                SIZE + 1, STATE + "_", new ArrayList<Tag>(), INSTANCE_ID + "_");
    }


    private Volume createVolume(String volId, String snapId, Date createDate, String zone, int size, String state, List<Tag> tags, String instance_id) {
        Volume volume = new Volume();
        volume.setVolumeId(volId);
        volume.setSnapshotId(snapId);
        volume.setCreateTime(createDate);
        volume.setAvailabilityZone(zone);
        volume.setState(state);
        volume.setSize(size);
        volume.setTags(tags);
        if (instance_id != null) {
            List<VolumeAttachment> volumeAttachmentList = new ArrayList();
            VolumeAttachment volumeAttachment = new VolumeAttachment();
            volumeAttachment.setInstanceId(instance_id);
            volumeAttachmentList.add(volumeAttachment);
            volume.setAttachments(volumeAttachmentList);
        }
        return volume;
    }

    private void assertVolumeDtoFields(VolumeDto volumeDto, String volId, String snapId, Date createDate, String zone, int size,
                                       String state, List<Tag> tags, String instance_id) {
        Assert.assertTrue(volumeDto.getVolumeId().equals(volId));
        Assert.assertTrue(volumeDto.getSnapshotId().equals(snapId));
        Assert.assertTrue(volumeDto.getCreateTime().equals(createDate));
        Assert.assertTrue(volumeDto.getAvailabilityZone().equals(zone));
        Assert.assertTrue(volumeDto.getState().equals(state));
        Assert.assertTrue(volumeDto.getSize() == size);
        Assert.assertTrue(volumeDto.getTags().equals(tags));
        if (instance_id != null) {
            Assert.assertTrue(volumeDto.getInstanceID().equals(instance_id));
        }
    }


}


