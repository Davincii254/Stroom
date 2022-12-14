import { useMemo } from "react";
import { Column } from "react-table";
import moment from "moment";
import useDateUtil from "../../../lib/useDateUtil";
import { Account } from "api/stroom";

moment.updateLocale("en", {
  invalidDate: "No date",
});

const useColumns = (): Column<Account>[] => {
  const { toDateString } = useDateUtil();
  return useMemo(
    () => [
      {
        Header: "",
        accessor: "id",
        // Cell: IdCell,
        // filterable: false,
        show: false,
      },
      {
        id: "userId",
        Header: "User Id",
        accessor: "userId",
        maxWidth: 150,
        // filterMethod: (filter: any, row: any) => filterRow(row, filter),
      },
      {
        Header: "Email",
        accessor: "email",
        maxWidth: 200,
        // filterMethod: (filter: any, row: any) => filterRow(row, filter),
      },
      {
        id: "status",
        Header: "Status",
        accessor: ({ locked, inactive, enabled }) =>
          locked
            ? "Locked"
            : inactive
            ? "Inactive"
            : enabled
            ? "Enabled"
            : "Disabled",
        maxWidth: 100,
        // Cell: StateCell,
        // Filter: getStateCellFilter,
      },
      {
        id: "lastLoginMs",
        Header: "Last Sign In",
        accessor: ({ lastLoginMs }) => lastLoginMs && toDateString(lastLoginMs),
        // Cell: (row: RowInfo) => moment(row.row.value).format(dateFormat),
        width: 165,
        maxWidth: 165,
        // filterable: false,
      },
      {
        Header: "Sign In Failures",
        accessor: "loginFailures",
        maxWidth: 120,
      },
      {
        Header: "Comments",
        accessor: "comments",
        maxWidth: 1000,
        // filterMethod: (filter: any, row: any) => filterRow(row, filter),
      },
    ],
    [toDateString],
  );
};

export default useColumns;
