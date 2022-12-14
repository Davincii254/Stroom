import { storiesOf } from "@storybook/react";
import * as React from "react";
import { ConfirmCurrentPasswordForm } from "./ConfirmCurrentPassword";
import { Formik } from "formik";
import * as Yup from "yup";
import { ResizableDialog } from "components/Dialog/ResizableDialog";

const FormikWrapper: React.FunctionComponent = () => {
  return (
    <Formik
      initialValues={{ userId: "", password: "" }}
      validationSchema={Yup.object().shape({
        password: Yup.string().required("Password is required"),
      })}
      onSubmit={(values, actions) => {
        setTimeout(() => {
          alert(JSON.stringify(values, null, 2));
          actions.setSubmitting(false);
        }, 1000);
      }}
    >
      {(formikProps) => (
        <ConfirmCurrentPasswordForm
          formikProps={formikProps}
          okCancelProps={{}}
        />
      )}
    </Formik>
  );
};

export const ConfirmCurrentPasswordDialog: React.FunctionComponent = () => {
  return (
    <ResizableDialog
      initWidth={400}
      initHeight={224}
      minWidth={400}
      minHeight={224}
      disableResize={true}
    >
      <FormikWrapper />
    </ResizableDialog>
  );
};

const TestHarness: React.FunctionComponent = () => {
  return <ConfirmCurrentPasswordDialog />;
};

storiesOf("Authentication", module).add("Confirm Current Password", () => (
  <TestHarness />
));
