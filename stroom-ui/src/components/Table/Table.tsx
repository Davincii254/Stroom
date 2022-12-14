import * as React from "react";

import { PropsWithChildren, ReactElement, useEffect, useMemo } from "react";
import {
  useTable,
  useBlockLayout,
  useResizeColumns,
  useRowSelect,
  useSortBy,
} from "react-table";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { CriteriaFieldSort } from "api/stroom";

export interface TableProps<T> {
  columns: any[];
  data: T[];
  selectedRows?: T[];
  setSelectedRows?: (selected: T[]) => void;
  initialSortBy?: CriteriaFieldSort[];
  onChangeSort?: (sort: CriteriaFieldSort[]) => void;
  onSelect?: (selected: T[]) => void;
  onDoubleSelect?: (selected: T[]) => void;
}

export const Table = <T,>(
  props: PropsWithChildren<TableProps<T>>,
): ReactElement<any, any> | null => {
  const {
    columns,
    data,
    selectedRows = [],
    setSelectedRows = () => undefined,
    initialSortBy = [],
    onChangeSort = () => undefined,
    onSelect = () => undefined,
    onDoubleSelect = () => undefined,
  } = props;

  const defaultColumn = useMemo(
    () => ({
      minWidth: 30,
      width: 150,
      maxWidth: 400,
    }),
    [],
  );

  const initialState = useMemo(() => {
    return {
      hiddenColumns: ["id"],
      sortBy: initialSortBy,
    };
  }, [initialSortBy]);

  const {
    getTableProps,
    getTableBodyProps,
    headerGroups,
    rows,
    prepareRow,
    state: { sortBy },
  } = useTable(
    {
      columns,
      data,
      defaultColumn,
      initialState,
      manualSortBy: true,
    },
    useBlockLayout,
    useResizeColumns,
    useSortBy,
    useRowSelect,
  );

  useEffect(() => {
    if (sortBy !== initialSortBy) {
      onChangeSort(sortBy);
    }
  }, [sortBy, initialSortBy, onChangeSort]);

  return (
    <div className="table sticky w-100 h-100">
      <div className="header" {...getTableProps()}>
        {headerGroups.map((headerGroup) => (
          <div
            key={headerGroup.id}
            {...headerGroup.getHeaderGroupProps()}
            className="tr"
          >
            {headerGroup.headers.map((column) => (
              <div key={column.id} {...column.getHeaderProps()} className="th">
                <div className="cell" {...column.getSortByToggleProps()}>
                  {column.render("Header")}
                  {column.isSorted ? (
                    column.isSortedDesc ? (
                      <FontAwesomeIcon icon={"sort-alpha-down-alt"} size="lg" />
                    ) : (
                      <FontAwesomeIcon icon={"sort-alpha-down"} size="lg" />
                    )
                  ) : (
                    ""
                  )}
                </div>
                {/* Use column.getResizerProps to hook up the events correctly */}
                {column.canResize && (
                  <div
                    {...column.getResizerProps()}
                    className={`resizer ${
                      column.isResizing ? "isResizing" : ""
                    }`}
                  />
                )}
              </div>
            ))}
          </div>
        ))}
      </div>
      <div {...getTableBodyProps()} className="body">
        {rows.map((row) => {
          prepareRow(row);
          return (
            <div
              key={row.id}
              {...row.getRowProps()}
              className={`tr ${
                selectedRows.indexOf(row.original) != -1 ? "selected" : ""
              }`}
              onClick={() => {
                setSelectedRows([row.original]);
                onSelect([row.original]);
              }}
              onDoubleClick={() => {
                setSelectedRows([row.original]);
                onDoubleSelect([row.original]);
              }}
            >
              {row.cells.map((cell) => (
                <div key={cell.id} {...cell.getCellProps()} className="td">
                  <div className="cell">{cell.render("Cell")}</div>
                </div>
              ))}
            </div>
          );
        })}
      </div>
    </div>
  );
};
