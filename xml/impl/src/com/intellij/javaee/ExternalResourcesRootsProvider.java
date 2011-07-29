/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.javaee;

import com.intellij.codeInsight.daemon.impl.quickfix.FetchExtResourceAction;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.IndexableSetContributor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public class ExternalResourcesRootsProvider extends IndexableSetContributor {

  private final NotNullLazyValue<Set<String>> myStandardResources = new NotNullLazyValue<Set<String>>() {
    @NotNull
    @Override
    protected Set<String> compute() {
      ExternalResourceManagerImpl manager = (ExternalResourceManagerImpl)ExternalResourceManagerEx.getInstance();
      Collection<Map<String,ExternalResourceManagerImpl.Resource>> resources = manager.getStandardResources();
      Set<ExternalResourceManagerImpl.Resource> dirs = new HashSet<ExternalResourceManagerImpl.Resource>();
      Set<String> set = new HashSet<String>();
      for (Map<String, ExternalResourceManagerImpl.Resource> map : resources) {
        for (ExternalResourceManagerImpl.Resource resource : map.values()) {
          ExternalResourceManagerImpl.Resource dir = new ExternalResourceManagerImpl.Resource();
          int i = resource.file.lastIndexOf('/');
          dir.file = i > 0 ? resource.file.substring(0, i) : resource.file;
          dir.classLoader = resource.classLoader;
          if (dirs.add(dir)) {
            String url = resource.getResourceUrl();
            if (url != null) {
              set.add(url.substring(0, url.lastIndexOf('/')));
            }
          }
        }
      }
      return set;
    }
  };

  public Set<VirtualFile> getAdditionalRootsToIndex() {

    HashSet<VirtualFile> roots = new HashSet<VirtualFile>();
    for (String url : myStandardResources.getValue()) {
      VirtualFile file = VfsUtil.findRelativeFile(url, null);
      if (file != null) {
        roots.add(file);
      }
    }

    String path = FetchExtResourceAction.getExternalResourcesPath();
    VirtualFile extResources = LocalFileSystem.getInstance().findFileByPath(path);
    ContainerUtil.addIfNotNull(extResources, roots);
    
    return roots;
  }
}
