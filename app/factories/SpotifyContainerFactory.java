package factories;

import models.*;
import play.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class SpotifyContainerFactory implements IContainerFactory<
        com.spotify.docker.client.messages.Container,
        com.spotify.docker.client.messages.ContainerStats> {

    /***
     * Translate containers from Spotify Docker containers to Containers
     * @param dockerContainerList Spotify docker containers
     * @param user User model
     * @return List of Container models
     */
    @Override
    public List<Container> translateContainers(List<com.spotify.docker.client.messages.Container> dockerContainerList,
                                               User user) {
        Logger.debug("Translate containers: " + user.tenant);
        List<Container> containerList = new ArrayList<>();
        if(dockerContainerList != null){
            dockerContainerList.forEach(container -> {
                String[] containerNameTokens = String.join("", container.names()).split(Pattern.quote("/"));
                String containerName = containerNameTokens[containerNameTokens.length - 1];

                Logger.debug("Name: " + containerName);
                if(containerName.startsWith("repose-" + user.tenant)) {
                    String reposeName = String.join(" ", container.names());
                    String[] reposeNames = reposeName.split(Pattern.quote("/"));
                    containerList.add(
                            new Container(
                                    reposeNames[reposeNames.length - 1],
                                    container.status().trim().startsWith("Up"),
                                    container.status(),
                                    reposeNames[reposeNames.length - 1].split(Pattern.quote("-"))[2],
                                    container.id()));
                }

            });
        }

        return containerList;
    }

    @Override
    public ContainerStats translateContainerStats(
            com.spotify.docker.client.messages.ContainerStats containerStats) {
        Logger.debug("Translate container stats");

        return new ContainerStats(
                translateCpuStats(containerStats.cpuStats()),
                translateMemoryStats(containerStats.memoryStats()),
                translateNetworkStats(containerStats.network()),
                translateCpuStats(containerStats.precpuStats())
                );
    }

    private CpuStats translateCpuStats(com.spotify.docker.client.messages.CpuStats cpuStats){
        return new CpuStats(translateCpuUsage(cpuStats.cpuUsage()), cpuStats.systemCpuUsage());
    }

    private CpuUsage translateCpuUsage(com.spotify.docker.client.messages.CpuUsage cpuUsage) {
        return new CpuUsage(
                cpuUsage.percpuUsage(), cpuUsage.totalUsage(),
                cpuUsage.usageInKernelmode(), cpuUsage.usageInUsermode());
    }

    private MemoryStats translateMemoryStats(com.spotify.docker.client.messages.MemoryStats memoryStats){
        return new MemoryStats(memoryStats.usage(), memoryStats.maxUsage(),
                memoryStats.limit(), memoryStats.failcnt());
    }

    private NetworkStats translateNetworkStats(com.spotify.docker.client.messages.NetworkStats networkStats){
        return new NetworkStats(networkStats.rxBytes(), networkStats.rxPackets(),
                networkStats.rxDropped(), networkStats.rxErrors(),
                networkStats.txBytes(), networkStats.txPackets(),
                networkStats.txDropped(), networkStats.txErrors());
    }
}
