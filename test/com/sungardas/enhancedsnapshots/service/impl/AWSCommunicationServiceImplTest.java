package com.sungardas.enhancedsnapshots.service.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.VolumeType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AWSCommunicationServiceImplTest {

    @Mock
    private AmazonEC2 ec2client;
    @InjectMocks
    private AWSCommunicationServiceImpl awsCommunticationService;
    private String snapshotId = "snap-1";
    private String availableZone = "zone-1";


    @Test(timeout = 3000)
    public void shouldCreateVolumeFromSnapshot() {
        CreateVolumeRequest crVolumeRequest = new CreateVolumeRequest(snapshotId,
                availableZone).withVolumeType("gp2");
        when(ec2client.createVolume(crVolumeRequest)).thenReturn(new CreateVolumeResult());
        awsCommunticationService.createVolumeFromSnapshot(snapshotId, availableZone, VolumeType.Gp2, 0);
        verify(ec2client, times(1)).createVolume(crVolumeRequest);
    }
}
