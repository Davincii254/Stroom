/*
 * Copyright 2018 Crown Copyright
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
import { StroomUser } from "components/AuthorisationManager/api/userGroups";
import { AnnotationsIndexDoc } from "components/DocumentEditors/useDocumentApi/types/annotations";
import {
  copyDocRef,
  DocRefTree,
} from "components/DocumentEditors/useDocumentApi/types/base";
import { DashboardDoc } from "components/DocumentEditors/useDocumentApi/types/dashboard";
import { DictionaryDoc } from "components/DocumentEditors/useDocumentApi/types/dictionaryDoc";
import { ElasticIndexDoc } from "components/DocumentEditors/useDocumentApi/types/elastic";
import { FeedDoc } from "components/DocumentEditors/useDocumentApi/types/feed";
import { FolderDoc } from "components/DocumentEditors/useDocumentApi/types/folder";
import { IndexDoc } from "components/DocumentEditors/useDocumentApi/types/indexDoc";
import { ScriptDoc } from "components/DocumentEditors/useDocumentApi/types/scriptDoc";
import {
  StatisticStoreDoc,
  StroomStatsStoreDoc,
} from "components/DocumentEditors/useDocumentApi/types/statistics";
import { VisualisationDoc } from "components/DocumentEditors/useDocumentApi/types/visualisation";
import { XMLSchemaDoc } from "components/DocumentEditors/useDocumentApi/types/xmlSchema";
import { XsltDoc } from "components/DocumentEditors/useDocumentApi/types/xsltDoc";
import { IndexVolumeGroup } from "components/IndexVolumes/indexVolumeGroupApi";
import { IndexVolume } from "components/IndexVolumes/indexVolumeApi";
import { StreamTaskType } from "components/Processing/types";
import { iterateNodes } from "lib/treeUtils";
import v4 from "uuid/v4";
import { TestData, UserDocPermission, UserGroupMembership } from "../testTypes";
import { generate as generateAnnotationsIndex } from "./annotationsIndex";
import allAppPermissions from "./appPermissions";
import { generate as generateDashboard } from "./dashboard";
import { dataList, dataSource } from "./data";
import { generate as generateDictionary } from "./dictionary";
import { documentPermissionNames } from "./docPermissions";
import testDocRefsTypes from "./docRefTypes";
import { generate as generateElasticIndex } from "./elasticIndex";
import { generate as generateFeed } from "./feed";
import { generate as generateIndex } from "./indexDocs";
import {
  generateTestIndexVolume,
  generateTestIndexVolumeGroup,
} from "./indexVolumes";
import { elementProperties, elements, testPipelines } from "./pipelines";
import { generate as generateScript } from "./script";
import { generate as generateStatisticStore } from "./statisticStore";
import { generate as generateStroomStatsStore } from "./stroomStatsStore";
import { generateGenericTracker } from "./trackers";
import { newToken } from "./tokens";
import {
  disabledUser,
  inactiveUser,
  lockedUser,
  newUser,
  wellUsedUser,
} from "./users";
import { generateTestGroup, generateTestUser } from "./usersAndGroups";
import { generate as generateVisualisation } from "./visualisation";
import { generate as generateXmlSchema } from "./xmlSchema";
import { generate as generateXslt } from "./xslt";

const docPermissionByType = testDocRefsTypes.reduce(
  (acc, curr) => ({ ...acc, [curr]: documentPermissionNames }),
  {},
);

const groups: StroomUser[] = Array(5).fill(1).map(generateTestGroup);
const stroomUsers: StroomUser[] = Array(30).fill(1).map(generateTestUser);
const userGroupMemberships: UserGroupMembership[] = [];
let userIndex = 0;
groups.forEach((group) => {
  for (let x = 0; x < 10; x++) {
    const user: StroomUser = stroomUsers[userIndex++];
    userIndex %= stroomUsers.length; // wrap
    userGroupMemberships.push({
      userUuid: user.uuid,
      groupUuid: group.uuid,
    });
  }
});
const allUsers = stroomUsers.concat(groups);
let permissionIndex = 0;
const userAppPermissions = {};
allUsers.forEach((u) => {
  const permissions = [];
  for (let x = 0; x < 2; x++) {
    permissions.push(allAppPermissions[permissionIndex++]);
    permissionIndex %= allAppPermissions.length; // wrap the index
  }
  userAppPermissions[u.uuid] = permissions;
});

const indexVolumeGroups: IndexVolumeGroup[] = Array(5)
  .fill(1)
  .map(generateTestIndexVolumeGroup);

const indexVolumes: IndexVolume[] = Array(30)
  .fill(1)
  .map(generateTestIndexVolume);

const annotationIndexes: AnnotationsIndexDoc[] = Array(3)
  .fill(1)
  .map(generateAnnotationsIndex);
const dashboards: DashboardDoc[] = Array(3).fill(1).map(generateDashboard);
const elasticIndexes: ElasticIndexDoc[] = Array(3)
  .fill(1)
  .map(generateElasticIndex);
const feeds: FeedDoc[] = Array(3).fill(1).map(generateFeed);
const scripts: ScriptDoc[] = Array(3).fill(1).map(generateScript);
const statisticStores: StatisticStoreDoc[] = Array(3)
  .fill(1)
  .map(generateStatisticStore);
const stroomStatsStores: StroomStatsStoreDoc[] = Array(3)
  .fill(1)
  .map(generateStroomStatsStore);
const visualisations: VisualisationDoc[] = Array(3)
  .fill(1)
  .map(generateVisualisation);
const xmlSchemas: XMLSchemaDoc[] = Array(3).fill(1).map(generateXmlSchema);

const dictionaries: DictionaryDoc[] = Array(5)
  .fill(null)
  .map(generateDictionary);

const xslt: XsltDoc[] = Array(5).fill(null).map(generateXslt);

const trackers: StreamTaskType[] = Array(10)
  .fill(null)
  .map(generateGenericTracker);

const indexes: IndexDoc[] = Array(5).fill(null).map(generateIndex);

// let users: User[] = Array(5)
// .fill(null)
// .map(generateUsers);

const docTree = {
  uuid: "0",
  type: "System",
  name: "System",
  children: [
    {
      uuid: v4(),
      name: "Raw Materials",
      type: "Folder",
      children: [
        {
          uuid: v4(),
          name: "Dictionaries",
          type: "Folder",
          children: dictionaries.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "XML_Schemas",
          type: "Folder",
          children: xmlSchemas.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "Feeds",
          type: "Folder",
          children: feeds.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "Scripts",
          type: "Folder",
          children: scripts.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "XSLT",
          type: "Folder",
          children: xslt.map(copyDocRef),
        },
      ],
    },
    {
      uuid: v4(),
      name: "Compound Stuff",
      type: "Folder",
      children: [
        {
          uuid: v4(),
          name: "Pipelines",
          type: "Folder",
          children: Object.values(testPipelines).map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "Dashboards",
          type: "Folder",
          children: dashboards.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "Indexes",
          type: "Folder",
          children: indexes.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "Annotation Indexes",
          type: "Folder",
          children: annotationIndexes.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "Elastic Indexes",
          type: "Folder",
          children: elasticIndexes.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "Visualisations",
          type: "Folder",
          children: visualisations.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "StatisticStore",
          type: "Folder",
          children: statisticStores.map(copyDocRef),
        },
        {
          uuid: v4(),
          name: "StroomStatsStore",
          type: "Folder",
          children: stroomStatsStores.map(copyDocRef),
        },
      ],
    },
    {
      uuid: v4(),
      name: "Empty Directory with a Long Name",
      type: "Folder",
      children: [],
    },
  ],
} as DocRefTree;

const userDocPermission: UserDocPermission[] = [];
const allFolders: FolderDoc[] = [];

// give first two users permissions to all documents
iterateNodes(docTree, (_, node) => {
  if (node.type === "Folder") {
    allFolders.push({
      type: "Folder",
      uuid: node.uuid,
      name: node.name,
      children: node.children || [],
    });
  }
  const { uuid: docRefUuid } = node;
  allUsers.slice(0, 2).forEach(({ uuid: userUuid }) => {
    documentPermissionNames
      .filter((p) => p !== "OWNER")
      .forEach((permissionName) => {
        userDocPermission.push({
          userUuid,
          docRefUuid,
          permissionName,
        });
      });
  });
});

export const fullTestData: TestData = {
  documentTree: docTree,
  docRefTypes: testDocRefsTypes,
  elements,
  elementProperties,
  documents: {
    XSLT: Object.values(xslt),
    Dictionary: Object.values(dictionaries),
    Feed: feeds,
    Index: indexes,
    Pipeline: Object.values(testPipelines),
    Folder: allFolders,
    AnnotationsIndex: annotationIndexes,
    Dashboard: dashboards,
    ElasticIndex: elasticIndexes,
    Script: scripts,
    StatisticStore: statisticStores,
    StroomStatsStore: stroomStatsStores,
    Visualisation: visualisations,
    XMLSchema: xmlSchemas,
  },
  dataList,
  dataSource,
  trackers,
  usersAndGroups: {
    users: allUsers,
    userGroupMemberships,
  },
  indexVolumesAndGroups: {
    volumes: indexVolumes,
    groups: indexVolumeGroups,
  },
  allAppPermissions,
  userAppPermissions,
  docPermissionByType,
  userDocPermission,
  tokens: [newToken],
  users: [
    disabledUser,
    inactiveUser,
    lockedUser,
    newUser,
    wellUsedUser,
    disabledUser,
    inactiveUser,
    lockedUser,
    newUser,
    wellUsedUser,
    disabledUser,
    inactiveUser,
    lockedUser,
    newUser,
    wellUsedUser,
    disabledUser,
    inactiveUser,
    lockedUser,
    newUser,
    wellUsedUser,
    disabledUser,
    inactiveUser,
    lockedUser,
    newUser,
    wellUsedUser,
  ],
};

export default fullTestData;
