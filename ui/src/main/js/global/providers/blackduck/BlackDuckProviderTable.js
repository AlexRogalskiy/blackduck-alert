import React, { useEffect, useRef, useState } from 'react';
import {
    BootstrapTable, DeleteButton, InsertButton, TableHeaderColumn
} from 'react-bootstrap-table';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { NavLink, Route } from 'react-router-dom';
import * as PropTypes from 'prop-types';
import AutoRefresh from 'component/common/AutoRefresh';
import * as ConfigRequestBuilder from 'util/configurationRequestBuilder';
import { BLACKDUCK_GLOBAL_FIELD_KEYS, BLACKDUCK_INFO, BLACKDUCK_URLS } from 'global/providers/blackduck/BlackDuckModel';
import * as FieldModelUtilities from 'util/fieldModelUtilities';
import ConfirmModal from 'component/common/ConfirmModal';
import IconTableCellFormatter from '../../../component/common/IconTableCellFormatter';

const BlackDuckProviderTable = ({ csrfToken, readonly, shouldRefresh }) => {
    const [tableData, setTableData] = useState([]);
    const [selectedConfigs, setSelectedConfigs] = useState([]);
    const [showDelete, setShowDelete] = useState(false);
    const [selectedRow, setSelectedRow] = useState(null);
    const tableRef = useRef();

    const readRequest = () => ConfigRequestBuilder.createReadAllGlobalContextRequest(csrfToken, BLACKDUCK_INFO.key);
    const deleteRequest = (id) => ConfigRequestBuilder.createDeleteRequest(ConfigRequestBuilder.CONFIG_API_URL, csrfToken, id);

    const retrieveTableData = async () => {
        const response = await readRequest();
        const data = await response.json();

        const { fieldModels } = data;
        const filteredFieldModels = fieldModels.filter((model) => FieldModelUtilities.hasAnyValuesExcludingId(model));
        const convertedTableDate = filteredFieldModels.map((fieldModel) => ({
            id: FieldModelUtilities.getFieldModelId(fieldModel),
            name: FieldModelUtilities.getFieldModelSingleValue(fieldModel, BLACKDUCK_GLOBAL_FIELD_KEYS.name),
            enabled: FieldModelUtilities.getFieldModelBooleanValue(fieldModel, BLACKDUCK_GLOBAL_FIELD_KEYS.enabled),
            lastUpdated: fieldModel.lastUpdated,
            createdAt: fieldModel.createdAt
        }));
        setTableData(convertedTableDate);
    };

    const deleteTableData = () => {
        if (selectedConfigs) {
            selectedConfigs.forEach((config) => {
                const configId = FieldModelUtilities.getFieldModelId(config);
                deleteRequest(configId);
            });
        }
        retrieveTableData();
        setSelectedConfigs([]);
        setShowDelete(false);
    };

    useEffect(() => {
        const fetchData = retrieveTableData;
        fetchData();
    }, []);

    const insertAndDeleteButton = (
        <div>
            <NavLink to={BLACKDUCK_URLS.blackDuckConfigUrl} activeClassName="addJobButton btn-md">
                <InsertButton
                    id="blackduck-insert-button"
                    className="addJobButton btn-md"
                    onClick={() => null}
                >
                    <FontAwesomeIcon icon="plus" className="alert-icon" size="lg" />
                    New
                </InsertButton>
            </NavLink>
            <DeleteButton
                id="blackduck-delete-button"
                className="deleteJobButton btn-md"
                onClick={() => setShowDelete(true)}
            >
                <FontAwesomeIcon icon="trash" className="alert-icon" size="lg" />
                Delete
            </DeleteButton>
        </div>
    );

    const tableOptions = {
        btnGroup: () => insertAndDeleteButton,
        noDataText: 'No Data',
        clearSearch: true,
        // handleConfirmDeleteRow: this.collectItemsToDelete,
        defaultSortName: 'name',
        defaultSortOrder: 'asc'
        // onRowDoubleClick: this.editButtonClicked
    };

    const assignedDataFormat = (cell) => (
        <div title={(cell) ? cell.toString() : null}>
            {cell}
        </div>
    );

    const selectRow = {
        mode: 'checkbox',
        clickToSelect: true,
        bgColor(row, isSelect) {
            return isSelect && '#e8e8e8';
        }
    };

    const column = (header, value) => (
        <TableHeaderColumn
            key={header}
            dataField={header}
            searchable
            dataSort
            columnClassName="tableCell"
            tdStyle={{ whiteSpace: 'normal' }}
            dataFormat={assignedDataFormat}
        >
            {value}
        </TableHeaderColumn>
    );

    const editFormat = (cell) => {
        const icon = (cell) ? 'check' : 'times';
        const color = (cell) ? 'synopsysGreen' : 'synopsysRed';
        const className = `alert-icon ${color}`;

        return (
            <div className="btn btn-link jobIconButton">
                <FontAwesomeIcon icon={icon} className={className} size="lg" />
            </div>
        );
    };

    const createTableCellFormatter = (iconName, buttonText, clickFunction) => {
        const buttonId = buttonText.toLowerCase();
        return (cell, row) => (
            <IconTableCellFormatter
                id={`blackduck-${buttonId}-cell`}
                handleButtonClicked={clickFunction}
                currentRowSelected={row}
                buttonIconName={iconName}
                buttonText={buttonText}
            />
        );
    };

    const editButtonClicked = ({ id }) => {
        setSelectedRow(id);
        // Navigate to config page
    };

    const editColumnFormatter = () => createTableCellFormatter('pencil-alt', 'Edit', editButtonClicked);

    const copyButtonClicked = ({ id }) => {
        setSelectedRow(id);
        // Navigate to config page
    };

    const copyColumnFormatter = () => createTableCellFormatter('copy', 'Copy', copyButtonClicked);

    const createIconTableHeader = (dataFormat, text) => (
        <TableHeaderColumn
            key={`${text}Key`}
            dataField=""
            width="48"
            columnClassName="tableCell"
            dataFormat={dataFormat}
            thStyle={{ textAlign: 'center' }}
        >
            {text}
        </TableHeaderColumn>
    );

    return (
        <div>
            <div className="pull-right">
                <AutoRefresh startAutoReload={retrieveTableData} isEnabled={shouldRefresh} />
            </div>
            <ConfirmModal
                id="blackduck-delete-confirm-modal"
                title="Delete"
                affirmativeAction={deleteTableData}
                affirmativeButtonText="Confirm"
                negativeAction={() => setShowDelete(false)}
                negativeButtonText="Cancel"
                message="Are you sure you want to delete these items?"
                showModal={showDelete}
            />
            <BootstrapTable
                version="4"
                hover
                condensed
                containerClass="table"
                trClassName="tableRow"
                headerContainerClass="scrollable"
                bodyContainerClass="tableScrollableBody"
                data={tableData}
                selectRow={selectRow}
                ref={tableRef}
                options={tableOptions}
                search
                newButton={!readonly}
            >
                <TableHeaderColumn dataField="id" hidden isKey>Id</TableHeaderColumn>
                {column('name', 'Name')}
                {column('createdAt', 'Created At')}
                {column('lastUpdated', 'Last Updated')}
                <TableHeaderColumn dataField="enabled" dataFormat={editFormat}>Enabled</TableHeaderColumn>
            </BootstrapTable>
        </div>
    );
};

BlackDuckProviderTable.propTypes = {
    csrfToken: PropTypes.string.isRequired,
    // Pass this in for now while we have all descriptors in global state, otherwise retrieve this in this component
    readonly: PropTypes.bool,
    shouldRefresh: PropTypes.bool
};

BlackDuckProviderTable.defaultProps = {
    readonly: false,
    shouldRefresh: false
};

export default BlackDuckProviderTable;
