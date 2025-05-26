/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.ecuacion.tool.housekeepfiles.bean;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

/**
 * Provides connection to remote server through sftp.
 */
public class ConnectionToSftpServer extends ConnectionToRemoteServer {
  private Session sftpSession;
  private ChannelSftp sftpChannel;

  /**
   * Construct a new instance.
   */
  public ConnectionToSftpServer(Session sftpSession, ChannelSftp sftpConnection) {
    this.sftpSession = sftpSession;
    this.sftpChannel = sftpConnection;
  }

  public ChannelSftp getSftpChannel() {
    return sftpChannel;
  }

  public void setSftpChannel(ChannelSftp sftpChannel) {
    this.sftpChannel = sftpChannel;
  }

  @Override
  public void closeConnection() {
    if (sftpChannel != null) {
      sftpChannel.disconnect();
    }

    if (sftpSession != null) {
      sftpSession.disconnect();
    }
  }
}
